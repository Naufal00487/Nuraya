package com.nuraya.app.study

import org.json.JSONArray
import org.json.JSONObject

internal object LearningCodec {
    private fun JSONArray.strings() = List(length()) { getString(it) }
    fun decode(data: JSONObject?): LearningProgress {
        if(data==null)return LearningProgress()
        require(data.getInt("version")==1)
        val daily=data.getJSONObject("daily")
        val records=data.getJSONArray("records")
        val active=data.optJSONObject("active")?.let { row ->
            val answers=row.getJSONArray("answers");val retries=row.getJSONArray("retries")
            LearningSession(row.getString("id"),row.getString("lesson"),row.getInt("seed"),row.getInt("base"),row.getInt("step"),
                List(answers.length()){answers.getJSONObject(it).let { answer -> LearningAnswer(answer.getBoolean("correct"),answer.getJSONArray("response").strings(),answer.getBoolean("scored")) }},
                List(retries.length()){retries.getInt(it)}).also {
                    require(it.baseCount in 1..12 && it.step in 0 until it.totalSteps && it.answers.size in it.step..it.step+1)
                    require(it.retryIndices.distinct()==it.retryIndices && it.retryIndices.all { index -> index in 0 until it.baseCount })
                }
        }
        val result=data.optJSONObject("result")?.let { LearningResult(it.getString("id"),it.getString("lesson"),it.getInt("correct"),it.getInt("total"),it.getInt("xp"),it.getInt("reviewed")) }
        return LearningProgress(data.getInt("xp"),data.getInt("goal"),data.getBoolean("goalEnabled"),
            daily.keys().asSequence().associateWith{daily.getInt(it)},
            List(records.length()){records.getJSONObject(it).let { row -> LessonRecord(row.getString("lesson"),row.getInt("attempts"),row.getInt("best"),row.getInt("total"),row.getString("date")) }},active,result)
    }
    fun encode(state: LearningProgress) = JSONObject().apply {
        put("version",1);put("xp",state.xp);put("goal",state.goal);put("goalEnabled",state.goalEnabled)
        put("daily",JSONObject(state.dailyLessons))
        put("records",JSONArray().apply { state.records.forEach { r -> put(JSONObject().apply {
            put("lesson",r.lessonId);put("attempts",r.attempts);put("best",r.bestCorrect);put("total",r.total);put("date",r.lastDate)
        }) } })
        state.active?.let { s -> put("active",JSONObject().apply {
            put("id",s.id);put("lesson",s.lessonId);put("seed",s.seed);put("base",s.baseCount);put("step",s.step)
            put("retries",JSONArray(s.retryIndices));put("answers",JSONArray().apply { s.answers.forEach { a -> put(JSONObject().apply {
                put("correct",a.correct);put("response",JSONArray(a.response));put("scored",a.scored)
            }) } })
        }) }
        state.result?.let { r -> put("result",JSONObject().apply {
            put("id",r.sessionId);put("lesson",r.lessonId);put("correct",r.correct);put("total",r.total);put("xp",r.xp);put("reviewed",r.reviewed)
        }) }
    }
}
