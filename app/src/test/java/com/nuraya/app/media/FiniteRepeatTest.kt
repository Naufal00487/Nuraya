package com.nuraya.app.media
import org.junit.Assert.*
import org.junit.Test
class FiniteRepeatTest {
    @Test fun alMulkOneToFiveRepeatsTheWholeRangeThreeTimesThenStops(){
        val queue=repeatedVerseSequence(1,5,1,3,30)
        assertEquals(listOf(1,2,3,4,5,1,2,3,4,5,1,2,3,4,5),queue)
        assertEquals(15,queue.size)
    }
    @Test fun eachVerseModeHasDifferentOrderAndWholeSurahKeepsFinalVerse(){
        assertEquals(listOf(1,1,1,2,2,2),repeatedVerseSequence(1,2,3,1,7))
        val queue=repeatedVerseSequence(1,30,1,2,30)
        assertEquals((1..30).toList()+(1..30).toList(),queue)
        assertEquals(30,queue.last())
    }
    @Test fun invalidRangesAndExcessiveQueueSizesAreRejected(){
        listOf({repeatedVerseSequence(0,5,1,3,30)},{repeatedVerseSequence(1,31,1,3,30)},{repeatedVerseSequence(5,1,1,3,30)},{repeatedVerseSequence(1,5,1,0,30)},{repeatedVerseSequence(1,30,20,20,30)}).forEach{action->try{action();fail("invalid queue accepted")}catch(_:IllegalArgumentException){}}
    }
}
