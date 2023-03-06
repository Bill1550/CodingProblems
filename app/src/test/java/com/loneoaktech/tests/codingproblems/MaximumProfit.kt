package com.loneoaktech.tests.codingproblems

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.lang.Long.max
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals

@Suppress("MemberVisibilityCanBePrivate") // this didn't seem to be working correctly
class MaximumProfit {
    /**
     * An analyst is analyzing a stock over a period of n days. The price of the stock on
     * the ith day is price[i], and the profit obtained is denoted by profit[i]. The analyst
     * wants to pick a triplet of days (i, j, k) such that (i < j < k) and
     * price[i] < price[j] < price[k] in such a way that the total profit,
     * i.e. profit[i] + profit[j] + profit[k] is maximized.
     * Find the maximum total profit possible. If there is no valid triplet, return -1.
     * Example
     *
     * Consider n = 5, price = [1, 5, 3, 4, 6], profit = [2, 3, 4, 5, 6].
     *
     * An optimal triplet (considering 1-based indexing) is (3, 4, 5).
     * Here 3 < 4 < 6, and total profit = 4 + 5 + 6 = 15, the maximum possible.
     * So, the answer is 15.
     *
     * Create a function:
     *  (size of prices and profits is the same.)
     *
     * fun getMaxProfit( prices: List<Int>, profits: List<Int> ): Long
     *
     * Lets make the function definition a bit better designed and more kotlin:
     * (guaranteed to have a 1-1 matching of prices to profits.
     *
     * fun findMaxProfile(  pricesToProfits: Pair<Int,Int> ): Long
     *
     */

    val sample1 = listOf(
        1 to 2,
        5 to 3,
        3 to 4,
        4 to 5,
        6 to 6
    )

    val sample2 = listOf(
        2 to 1,
        3 to 2,
        1 to 6,
        5 to 1,
        9 to 5
    )

    val sample3 = listOf(
        4 to 4,
        3 to 3,
        2 to 2,
        1 to 1
    )

    val sample4 = listOf(
        1 to 1,
        2 to 2,
        3 to 3,
        4 to 4,
        5 to 5,
        6 to 6,
        7 to 7,
        8 to 8,
        9 to 9,
        10 to 10
    )

    val tests = listOf(
        sample1 to 15,
        sample2 to 12,
        sample3 to -1,
        sample4 to 27
    )

    //---- Solution ----

    // Basic strategy:
    //    1. Create a mechanism to iterate over all the possible triplets. Since
    //       the triplet indexes (i,j,k) must obey i < j < k, the total number of
    //       possible triplets is less the the combinatorial number, so the method
    //       should take advantage of the i<j<k when generating test cases.
    //
    //    2. Once we can iterate of over the valid index triplets, then extract
    //       the prices and test to see if they pass the  price[i] < price[j] < price[k]
    //
    //    3. then sum the profits for the triplets that meet #2, and find the max.

    /**
     * Create a list of index triples in a way that
     * the integration algorithm can be separately tested
     * from the price/profit logic
     */
    fun generateAscendingTripleIndexes(domain: Int): List<List<Int>> {
        if (domain < 3)
            throw IllegalArgumentException("Input list size ($domain must be >= e")

        val results = mutableListOf<List<Int>>()

        for( i in 0..(domain-3) )
            for( j in i+1..(domain-2))
                for( k in j+1 until domain){
                    results.add( listOf(i,j,k))
                }

        return results
    }

    /**
     * Possibly a better solution:
     * Iterate over the indexes w/o generating lists (save a bit of memory).
     * But still in a way that can be tested separately.
     *
     * (interestingly, in the tests below, this ran slightly slower than the list version.
     *  though the test list lengths are fairly low, could more of a difference for very
     *  large data sets)
     */
    fun foldAscendingTripleIndexes(domain: Int, folder: (List<Int>)->Unit ) {
        if (domain < 3)
            throw IllegalArgumentException("Input list size ($domain must be >= e")

        for( i in 0..(domain-3) )
            for( j in i+1..(domain-2))
                for( k in j+1 until domain){
                    folder(listOf(i,j,k))
                }
    }

    private val expectedD4Triples = listOf( listOf(0,1,2), listOf(0,1,3), listOf(0,2,3), listOf(1,2,3))

    @Test
    fun testIndexListGenerator() {
        assertEquals(expectedD4Triples, generateAscendingTripleIndexes(4))

        val expected3 = listOf( listOf(0,1,2))
        assertEquals(expected3, generateAscendingTripleIndexes(3))
    }


    @Test
    fun testIndexFoldGenerator() {
        val triples = mutableListOf<List<Int>>()

        foldAscendingTripleIndexes(4) { l -> triples.add(l) }
        assertEquals(expectedD4Triples, triples)
    }

    /**
     * Now create a method to extract the price/profit pairs by creating a new list by selecting
     * elements from 'this' list using a list of indexes
     */
    fun <T> List<T>.selectElements(ndx: List<Int>): List<T> {
        val results = mutableListOf<T>()
        for( n in ndx) {
            if (n !in this.indices)
                throw IllegalArgumentException( "Index $n not in input list")

            results.add(this[n])
        }
        return results
    }

    @Test
    fun testSelectElements() {
        val input = (0..10).toList()

        val r1 = input.selectElements(listOf(1,3,5))

        assertEquals(3, r1.size)
        assertEquals( listOf(1,3,5), r1 )
    }

    /**
     * Generic function that returns true if the elements of this list,
     * as returned by the selector, are in ascending order
     */
    fun <T> List<T>.isAscending(selector: (T)->Int ) : Boolean {
        if ( this.size < 2 )
            throw IllegalArgumentException("Size of input must be >= 2, found $[this.size}")
        for( i in 0 until this.size-1) {
            if ( selector.invoke(this[i]) >= selector(this[i+1]) )
                return false
        }
        return true
    }

    @Test
    fun testIsAscending() {
        val tests = listOf(
            listOf(1,2) to true,
            listOf(2,1) to false,
            listOf(1,2,3) to true,
            listOf(3,2,1) to false,
            listOf(3,3,3) to false,
            listOf(1,5,7) to true,
            listOf(1,2,3,4) to true,
            listOf(4,3,2,1) to false
        )

        tests.forEach { (l, r) ->
            assertEquals( r, l.isAscending { it }, "Failed on $l" )
        }
    }

    /**
     * A generic function that returns the sum of the selected elements of this list.
     */
    private fun <T> List<T>.sumSelection(selector: (T)->Int): Long = this.fold(0L){ accum, e ->
        accum + selector(e)
    }

    @Test
    fun testSumSelection() {

        val in1 = listOf( 1,2,3,4)
        assertEquals( 10, in1.sumSelection { it })

        assertEquals( 10, sample3.sumSelection { it.first })
        assertEquals( 10, sample3.sumSelection { it.second })
    }

    /**
     * Put the components above together to generate the answer.
     *
     * Finds the max profit for 3 selected days from the list of pairs
     * of (price, profit) where the price of each of the days is ascending
     * through the 3 days
     *
     */
    fun List<Pair<Int,Int>>.findMaxProfit(): Long {

        var maxProfit = -1L

        // This first generates a list of possible index triplets, then
        // tests the prices and computes the profits.
        generateAscendingTripleIndexes(this.size).forEach { ndx ->
            val triplet = this.selectElements(ndx)
            if ( triplet.isAscending { it.first }){
                val p = triplet.sumSelection { it.second }
                maxProfit = max(maxProfit.toLong(),p.toLong())
            }
        }

       return maxProfit
    }

    @Test
    fun testFindMaxProfit() {
        measureNanoTime {
            tests.forEach { (l, pr) ->
                assertEquals( pr.toLong(), l.findMaxProfit() )
            }
        }.let {
            println("List version took ${it/1000} for all tests")
        }

    }

    /**
     * Compute the max profit using the fold algorithm.
     */
    fun List<Pair<Int,Int>>.foldMaxProfit(): Long {

        var maxProfit = -1L

        // As valid triplets are created, immediately
        // test against the price rule and compute profit,
        // avoiding creating the list.
        // This essentially uses the List.fold technique, except
        // the accumulator is pass in a closure rather than through call arguments.
        foldAscendingTripleIndexes( this.size){ ndx ->
            val triplet = this.selectElements(ndx)
            if ( triplet.isAscending { it.first }){
                val p = triplet.sumSelection { it.second }
                maxProfit = max(maxProfit,p)
            }
        }

        return maxProfit
    }

    @Test
    fun testFoldMaxProfit() {
        measureNanoTime {
            tests.forEach { (l, pr) ->
                assertEquals( pr.toLong(), l.foldMaxProfit() )
            }
        }.let {
            println("Fold version took ${it/1000} for all tests")
        }
    }

    /**
     *  For the hard core - a flow version (uses less memory, for is slower.
     *  Might be a advantage if the data was coming from some source (i.e. database)
     *  rather than a simple list.
     */
    fun flowAscendingTripleIndexes(domain: Int): Flow<List<Int>> = flow {
        if (domain < 3)
            throw IllegalArgumentException("Input list size ($domain must be >= e")

        for( i in 0..(domain-3) )
            for( j in i+1..(domain-2))
                for( k in j+1 until domain){
                    emit( listOf(i,j,k))
                }
    }

    fun List<Pair<Int,Int>>.flowMaxProfit(): Long = runBlocking {
        val input = this@flowMaxProfit
        flowAscendingTripleIndexes(input.size)
            .map { input.selectElements(it) }
            .filter { it.isAscending { it.first } }
            .map { it.sumSelection { it.second } }
            .fold( -1L) { acc, l -> max(acc,l) }
    }

    @Test
    fun testFlowMaxProfit() {
        measureNanoTime {
            tests.forEach { (l, pr) ->
                assertEquals(pr.toLong(), l.flowMaxProfit())
            }
        }.let {
            println("Flow version took ${it/1000} microseconds for all tests")
            // Uses less memory (creates fewer lists) but is a lot slower.
        }

    }
}