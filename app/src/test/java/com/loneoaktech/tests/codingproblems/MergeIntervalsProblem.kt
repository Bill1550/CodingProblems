package com.loneoaktech.tests.codingproblems

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.lang.Integer.max
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals

/**
 * The problem is to sort and merge a series of time intervals.
 * The result should be a new list of intervals, sorted in order with all
 * adjacent intervals merged.
 */
class MergeIntervalsProblem {

    /**
     * Represent the time intervals using Kotlin IntRange
     */
    private val intervals1 = listOf(
        7..7,
        2..3,
        6..11,
        1..2,
        9..10
    )

    fun List<IntRange>.mergeIntervals() = this.sortedBy { it.first }.fold( listOf<IntRange>() ){ mergedList, np ->
        if ( mergedList.isEmpty() )
            listOf(np)
        else if ( np.first in mergedList.last() ) {
            val e = mergedList.last()
            mergedList.dropLast(1) + listOf( e.first .. max(e.last, np.last))
        }
        else
            mergedList + listOf(np)
    }


    fun Flow<IntRange>.conflateIntervals(): Flow<IntRange> = flow {
        var _accum: IntRange? = null
        this@conflateIntervals.collect { next ->
            _accum?.let { accum ->
                if ( next.start in accum )
                    _accum = IntRange(accum.first, max(accum.last, next.last))
                else {
                    emit(accum)
                    _accum = next
                }
            } ?: let {
                _accum = next
            }
            _accum?.let { emit(it) }
        }
    }

    @Test
    fun pureListSolution() {
        val intervals1Result = intervals1.mergeIntervals()

        println("result=$intervals1Result")
        assertEquals( listOf( 1..3, 6..11), intervals1Result)
    }

    @Test
    fun sortThenFlowSolution() {

        val results = runBlocking {
            intervals1.sortedBy { it.first }.asFlow().conflateIntervals().toList()
        }

        println(results)
    }

    fun Flow<IntRange>.conflateAndSortIntervals(): Flow<IntRange> = flow {
        var _accum: IntRange? = null
        this@conflateAndSortIntervals.collect { next ->
            _accum?.let { accum ->
                if ( next.start in accum ) {
                    // merge
                    _accum = IntRange(accum.first, max(accum.last, next.last))
                } else {
                    if (accum.first < next.first) {
                        emit(accum)
                        _accum = next
                    } else {
                        // swap
                        emit(next)
                    }
                }
            } ?: let {
                _accum = next
            }
        }

        _accum?.let { emit(it) }
    }

    fun Flow<IntRange>.print(): Flow<IntRange> = runBlocking {
        this@print.toList().apply {
            println("List: $this")
        }.asFlow()
    }

    @Test
    fun sortAndFlowSolution() {

        val results = runBlocking {
            intervals1.asFlow()
                .conflateAndSortIntervals()
                .print()
                .conflateAndSortIntervals()
                .print()
                .conflateAndSortIntervals()
                .print()
                .conflateAndSortIntervals()
                .toList()
        }

        println( "Results=$results")
    }



    @Test
    fun sortAndFlowAgainSolution() {

        var intervalFlow = intervals1.asFlow()

        val results = runBlocking {

            repeat(intervals1.size) {
                intervalFlow = intervalFlow.conflateAndSortIntervals()
            }

            intervalFlow.toList()
        }

        println("Results=$results")
    }

    fun List<IntRange>.flowSortMerge(buffered: Boolean=false): List<IntRange> = runBlocking {
        var intervalFlow = this@flowSortMerge.asFlow()

        repeat(this@flowSortMerge.size) {
            intervalFlow = intervalFlow.conflateAndSortIntervals()
            if ( buffered)
                intervalFlow = intervalFlow.buffer()
        }

        intervalFlow.toList()
    }

    @Test
    fun sortAndMergeTestRange() {

        val nullListResult = listOf<IntRange>().flowSortMerge()
        assertEquals(0, nullListResult.size, "empty list does not return an empty list")

        val singleArg = listOf( 1..5)
        val singleResult = singleArg.flowSortMerge()
        assertEquals(1, singleResult.size)
        assertEquals(singleArg, singleResult)

        val interval1Result = intervals1.flowSortMerge()
        assertEquals( listOf( 1..3, 6..11), interval1Result)

        val interval1Drop1Result = intervals1.drop(1).flowSortMerge()
        assertEquals( listOf( 1..3, 6..11), interval1Drop1Result)

    }

    @Test
    fun measureTime() {

        val unBufferedTime = measureNanoTime {
            val result = intervals1.flowSortMerge(buffered = false)
        }

        val bufferedTime = measureNanoTime {
            val result = intervals1.flowSortMerge(buffered = true)
        }

        println("not buffered=$unBufferedTime, buffered=$bufferedTime, buf/unbuf=${bufferedTime.toFloat()/unBufferedTime.toFloat()}")
    }

    /**
     *  Compare the execution speed of the various implementations.
     */
    @Test
    fun listVsFlow() {

        val unBufferedTime = measureNanoTime {
            val result = intervals1.flowSortMerge(buffered = false)
        }

        val bufferedTime = measureNanoTime {
            val result = intervals1.flowSortMerge(buffered = true)
        }

        val sortThenFLowTime = measureNanoTime {
            val results = runBlocking {
                intervals1.sortedBy { it.first }.asFlow().conflateIntervals().toList()
            }
        }

        val listTime = measureNanoTime {
            val result = intervals1.mergeIntervals()
        }

        println("unbuffered=$unBufferedTime, buffered=$bufferedTime, sortThenFlowTIme=$sortThenFLowTime, list=$listTime")
    }
}