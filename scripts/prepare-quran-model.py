#!/usr/bin/env python3
"""Fetch/verify pinned Quran ASR weights and convert them to whisper.cpp GGML.

The tensor mapping follows whisper.cpp v1.7.6 models/convert-h5-to-ggml.py
(MIT, see app/src/main/assets/licenses/WHISPER_MIT.txt). Nuraya modifications:
verify every source hash, load weights_only=True without Transformers or remote
code, derive text context from decoder positional tensors, preserve FP32 small
tensors rather than round-trip through FP16, and emit reproducibility metadata.
No model or inference result is fabricated when the download is unavailable.
"""
from __future__ import annotations
import argparse, hashlib, json, os, struct, subprocess, sys, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROVENANCE = ROOT / 'docs/sources/quran-ai/provenance.json'
MAPPING = {
    'self_attn.k_proj': 'attn.key', 'self_attn.q_proj': 'attn.query',
    'self_attn.v_proj': 'attn.value', 'self_attn.out_proj': 'attn.out',
    'self_attn_layer_norm': 'attn_ln', 'encoder_attn.q_proj': 'cross_attn.query',
    'encoder_attn.k_proj': 'cross_attn.key', 'encoder_attn.v_proj': 'cross_attn.value',
    'encoder_attn.out_proj': 'cross_attn.out', 'encoder_attn_layer_norm': 'cross_attn_ln',
    'fc1': 'mlp.0', 'fc2': 'mlp.2', 'final_layer_norm': 'mlp_ln',
    'encoder.layer_norm.bias': 'encoder.ln_post.bias', 'encoder.layer_norm.weight': 'encoder.ln_post.weight',
    'encoder.embed_positions.weight': 'encoder.positional_embedding',
    'decoder.layer_norm.bias': 'decoder.ln.bias', 'decoder.layer_norm.weight': 'decoder.ln.weight',
    'decoder.embed_positions.weight': 'decoder.positional_embedding',
    'decoder.embed_tokens.weight': 'decoder.token_embedding.weight',
}


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''): h.update(chunk)
    return h.hexdigest()


def checked(path: Path, size: int, sha: str) -> None:
    if not path.is_file() or path.stat().st_size != size or digest(path) != sha:
        raise SystemExit(f'Missing or invalid pinned source: {path}. Run --fetch after permitting the official download host.')


def fetch(path: Path, url: str, size: int, sha: str) -> None:
    if path.is_file() and path.stat().st_size == size and digest(path) == sha: return
    partial = path.with_name(path.name + '.part')
    # A bounded HTTPS request; follow the provider's HTTPS redirects using normal urllib TLS validation.
    with urllib.request.urlopen(url, timeout=30) as response, partial.open('wb') as f:
        if not response.geturl().startswith('https://'): raise SystemExit('Model redirect was not HTTPS')
        total = 0
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk: break
            total += len(chunk)
            if total > size: raise SystemExit('Source exceeds the pinned length')
            f.write(chunk)
        f.flush(); os.fsync(f.fileno())
    checked(partial, size, sha)
    partial.replace(path)


def tensor_name(name: str) -> str | None:
    if name == 'proj_out.weight': return None  # tied to decoder.token_embedding.weight
    if not name.startswith('model.'): raise ValueError(f'Unexpected tensor {name}')
    parts = name.split('.')[1:]
    if len(parts) > 2 and parts[1] == 'layers':
        parts[1] = 'blocks'
        return '.'.join(parts[:3] + [MAPPING['.'.join(parts[3:-1])]] + parts[-1:])
    plain = '.'.join(parts)
    return MAPPING.get(plain, plain)


def byte_decoder() -> dict[str, int]:
    values = list(range(33,127)) + list(range(161,173)) + list(range(174,256))
    codes = list(values)
    for value in range(256):
        if value not in values:
            values.append(value); codes.append(256 + sum(code >= 256 for code in codes))
    return {chr(code): value for value, code in zip(values, codes)}


def convert(folder: Path, destination: Path) -> None:
    import numpy as np
    import torch
    config = json.loads((folder / 'config.json').read_text())
    # Restricted unpickler: do not execute repository-specific model code or unrestricted pickle.
    state = torch.load(folder / 'pytorch_model.bin', map_location='cpu', weights_only=True)
    if not isinstance(state, dict) or not all(isinstance(v, torch.Tensor) for v in state.values()):
        raise ValueError('Expected a tensor-only state dictionary')
    text_shape = tuple(state['model.decoder.embed_positions.weight'].shape)
    if text_shape != (config['max_target_positions'], config['d_model']):
        raise ValueError('Decoder positional tensor does not match max_target_positions')
    # max_length=1024 in this checkpoint is a generation option, NOT its 448-token decoder context.
    hparams = [config['vocab_size'], config['max_source_positions'], config['d_model'],
               config['encoder_attention_heads'], config['encoder_layers'], text_shape[0],
               config['d_model'], config['decoder_attention_heads'], config['decoder_layers'],
               config['num_mel_bins'], 1]
    with np.load(folder / 'mel_filters.npz', allow_pickle=False) as data:
        filters = np.asarray(data[f"mel_{config['num_mel_bins']}"], dtype='<f4')
    vocab = json.loads((folder / 'vocab.json').read_text())
    tokens = sorted(vocab.items(), key=lambda pair: pair[1])
    if [i for _,i in tokens] != list(range(len(tokens))): raise ValueError('Nonconsecutive tokenizer IDs')
    decoder = byte_decoder()
    temporary = destination.with_suffix('.part')
    with temporary.open('wb') as out:
        out.write(struct.pack('<12i', 0x67676D6C, *hparams))
        out.write(struct.pack('<2i', *filters.shape)); out.write(filters.tobytes(order='C'))
        out.write(struct.pack('<i', len(tokens)))
        for token, _ in tokens:
            raw = bytes(decoder[c] for c in token)
            out.write(struct.pack('<i', len(raw))); out.write(raw)
        names = set()
        for original, tensor in state.items():
            name = tensor_name(original)
            if name is None: continue
            if name in names: raise ValueError('Duplicate destination tensor')
            names.add(name)
            values = tensor.detach().float().numpy().squeeze()
            if name in {'encoder.conv1.bias','encoder.conv2.bias'}: values = values.reshape(-1,1)
            f32 = values.ndim < 2 or name in {'encoder.conv1.bias','encoder.conv2.bias',
                'encoder.positional_embedding','decoder.positional_embedding'}
            dtype = '<f4' if f32 else '<f2'; kind = 0 if f32 else 1
            values = values.astype(dtype)
            raw_name = name.encode('utf-8')
            out.write(struct.pack('<3i', values.ndim, len(raw_name), kind))
            out.write(struct.pack('<' + 'i'*values.ndim, *reversed(values.shape)))
            out.write(raw_name); out.write(values.tobytes(order='C'))
        out.flush(); os.fsync(out.fileno())
    temporary.replace(destination)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', type=Path, default=Path('/workspace/.tools/quran-model'))
    parser.add_argument('--fetch', action='store_true')
    parser.add_argument('--convert', action='store_true')
    parser.add_argument('--quantizer', type=Path, help='Built whisper.cpp v1.7.6 quantize executable; emits Q5_1')
    parser.add_argument('--verify', action='store_true')
    args = parser.parse_args(); spec = json.loads(PROVENANCE.read_text()); folder=args.directory
    if args.quantizer and not args.convert:
        raise SystemExit('Pass --convert together with --quantizer so no stale intermediate model is used')
    folder.mkdir(parents=True,exist_ok=True)
    source = f"https://huggingface.co/{spec['model']}/resolve/{spec['revision']}/"
    entries = {**spec['files'], spec['weightFile']: {'bytes':spec['weightBytes'], 'sha256':spec['weightSha256']},
               'mel_filters.npz': spec['melFilters']}
    for name, entry in entries.items():
        path=folder/name
        if args.fetch: fetch(path, entry.get('url',source+name), entry['bytes'], entry['sha256'])
        checked(path,entry['bytes'],entry['sha256'])
    print('Verified every pinned source file, including the actual model weights.',flush=True)
    f16=folder/'nuraya-quran-whisper-base-f16.bin'
    if args.convert: convert(folder,f16)
    files=[]
    if args.convert: files.append(f16)
    if args.quantizer:
        if not f16.is_file(): raise SystemExit('Run --convert before quantization')
        target=folder/'nuraya-quran-whisper-base-q5_1.bin'
        subprocess.run([str(args.quantizer),str(f16),str(target),'q5_1'],check=True)
        files.append(target)
    if files:
        results={'sourceModel':spec['model'],'sourceRevision':spec['revision'],
                 'sourceWeightSha256':spec['weightSha256'],'conversion':'GGML f16 with 448 text context; optional Q5_1',
                 'files':[{'file':p.name,'bytes':p.stat().st_size,'sha256':digest(p)} for p in files],
                 'inferenceValidated':False}
        (folder/'conversion.json').write_text(json.dumps(results,indent=2)+'\n')
        print(json.dumps(results,indent=2))

if __name__=='__main__': main()
