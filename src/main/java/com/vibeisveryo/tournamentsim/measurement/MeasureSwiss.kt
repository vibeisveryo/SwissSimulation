/*
 * Copyright 2022, 2023 vorboyvo
 *
 * This file is part of TournamentSimulation.
 *
 * TournamentSimulation is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * TournamentSimulation is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with TournamentSimulation. If not, see
 * <https://www.gnu.org/licenses/>.
 */
package com.vibeisveryo.tournamentsim.measurement

import com.vibeisveryo.tournamentsim.simulation.Division
import com.vibeisveryo.tournamentsim.simulation.Division.SkillStyle
import com.vibeisveryo.tournamentsim.tournament.Swiss
import com.vibeisveryo.tournamentsim.util.OutWriter
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object MeasureSwiss {
    @JvmStatic
    @Throws(IOException::class)
    fun measureDistortionsOverMatches(
        matchStart: Int, matchStop: Int, teamCount: Int, iterations: Int,
        skillStyle: SkillStyle
    ) {
        val outWriter = OutWriter("distortions_matches", "matches", "distortions")

        // Do the iters
        for (i in 0 until iterations) {
            for (matchCount in matchStart until matchStop) {
                val main = Division("Main", teamCount, skillStyle)
                Swiss.swissRunMatches(main, matchCount)
                val distortions = Distortions.getDistortions(main)
                outWriter.print(
                    matchCount, String.format("%3.5f", Distortions.sumDistortionsPerTeam(distortions))
                )
            }
        }
        // Close writer
        outWriter.close()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun measureCombinedDistortions(
        matchesStart: Int, teamsStart: Int, teamsStop: Int,
        iterations: Int, skillStyle: SkillStyle
    ) {
        val outWriter = OutWriter("distortions_combined", "matches", "teams", "distortions")

        // Do the iters
        for (i in 0 until iterations) {
            val startTime = Instant.now()
            for (teamCount in teamsStart .. teamsStop) {
                for (matchCount in matchesStart .. (ceil(teamCount / 2.0) * 2 - 3).toInt()) {
                    val main = Division("Main", teamCount, skillStyle)
                    Swiss.swissRunMatches(main, matchCount)
                    val distortions = Distortions.getDistortions(main)
                    outWriter.addRecord(
                        matchCount,
                        teamCount,
                        String.format("%3.5f", Distortions.sumDistortionsPerTeam(distortions))
                    )
                }
            }
            outWriter.print()
            val endTime = Instant.now()
            val time = Duration.between(startTime, endTime).toNanos()
            if (i % 10.0.pow(floor(log10((iterations - 1).toDouble()))) == 0.0
                || time > TimeUnit.SECONDS.toNanos(1L)
            ) System.out.printf("Iteration %d took %4.5f seconds\n", i, time / 1000000000.0)
        }

        // Output CSV
        outWriter.close()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun measureCombinedDistortionsTwoMatches(
        weeksStart: Int, teamsStart: Int, teamsStop: Int,
        iterations: Int, skillStyle: SkillStyle
    ) {
        val outWriter = OutWriter("distortions_combined_double", "matches", "teams", "distortions")

        // Do the iters
        for (i in 0 until iterations) {
            val startTime = Instant.now()
            for (teamCount in teamsStart .. teamsStop) {
                for (weekCount in weeksStart .. (ceil(teamCount / 2.0) - 2).toInt()) {
                    val main = Division("Main", teamCount, skillStyle)
                    Swiss.swissRunTupleMatches(main, weekCount,2)
                    val distortions = Distortions.getDistortions(main)
                    outWriter.addRecord(
                        weekCount*2,
                        teamCount,
                        String.format("%3.5f", Distortions.sumDistortionsPerTeam(distortions))
                    )
                }
            }
            outWriter.print()
            val endTime = Instant.now()
            val time = Duration.between(startTime, endTime).toNanos()
            if (i % 10.0.pow(floor(log10((iterations - 1).toDouble()))) == 0.0
                || time > TimeUnit.SECONDS.toNanos(1L)
            ) System.out.printf("Iteration %d took %4.5f seconds\n", i, time / 1000000000.0)
        }

        // Output CSV
        outWriter.close()
    }

    @Throws(IOException::class)
    fun measureCombinedFractionalDistortions(
        matchesStart: Int, matchesStop: Int, teamsStart: Int,
        teamsStop: Int, iterations: Int,
        skillStyle: SkillStyle?
    ) {
        val outWriter = OutWriter(
            "distortions_combined", "matches", "teams",
            "distortionstophalf", "distortionstoptwothirds", "distortions"
        )

        // Do the iters
        for (i in 0 until iterations) {
            val startTime = Instant.now()
            for (teamCount in teamsStart .. teamsStop) {
                var matchCount = matchesStart
                while (matchCount < (if (matchesStop < 0) (ceil(teamCount / 2.0) * 2 - 2).toInt() else matchesStop)) {
                    val main = Division("Main", teamCount, skillStyle!!)
                    Swiss.swissRunMatches(main, matchCount)
                    val distortions = Distortions.getDistortions(main)
                    outWriter.addRecord(
                        matchCount,
                        teamCount,
                        String.format("%3.5f", Distortions.sumFractionalDistortions(distortions, 1 / 2.0)),
                        String.format("%3.5f", Distortions.sumFractionalDistortions(distortions, 1 / 3.0)),
                        String.format("%3.5f", Distortions.sumDistortionsPerTeam(distortions))
                    )
                    matchCount++
                }
            }
            outWriter.print()
            val endTime = Instant.now()
            val time = Duration.between(startTime, endTime).toNanos()
            if (i % 10.0.pow(floor(log10((iterations - 1).toDouble()))) == 0.0
                || time > TimeUnit.SECONDS.toNanos(1L)
            ) System.out.printf("Iteration %d took %4.5f seconds\n", i, time / 1000000000.0)
        }

        // Output CSV
        outWriter.close()
    }

    @Throws(IOException::class)
    fun getStandingsOverASeason(iterations: Int, matchCount: Int, teamCount: Int) {
        val outWriter =
            OutWriter("standings_weeks_${teamCount}teams", "week", "skillRank", "leagueTableRank")
        for (i in 0 until iterations) {
            val startTime = Instant.now()
            val main = Division("Main", teamCount, SkillStyle.UNIFORM)
            for (week in 0 until matchCount) {
                Swiss.swissRunMatches(main, 1)
                // Get team skill rank
                val teamSkillRanks = main.teamSkillRanks()
                for (j in 0 until teamCount) {
                    outWriter.addRecord(week, teamSkillRanks[j], j)
                }
            }
            outWriter.print()
            val endTime = Instant.now()
            val time = Duration.between(startTime, endTime).toNanos()
            if (i % 10.0.pow(floor(log10((iterations - 1).toDouble()))) == 0.0
                || time > TimeUnit.SECONDS.toNanos(1L)
            ) System.out.printf("Iteration %d took %4.5f seconds\n", i, time / 1000000000.0)
        }
        outWriter.close()
    }
}