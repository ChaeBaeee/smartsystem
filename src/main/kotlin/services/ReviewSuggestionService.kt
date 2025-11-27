package com.smartstudy.services

import com.smartstudy.data.DataManager
import com.smartstudy.models.Topic

class ReviewSuggestionService {
    
    /**
     * Get topics that should be reviewed, sorted by priority
     */
    fun getSuggestedTopics(subjectId: String? = null, limit: Int = 10): List<TopicSuggestion> {
        val topics = if (subjectId != null) {
            DataManager.getTopics().filter { it.subjectId == subjectId }
        } else {
            DataManager.getTopics()
        }
        
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        
        return topics.mapNotNull { topic ->
            val daysSinceReview = if (topic.lastReviewed != null) {
                val days = (now - topic.lastReviewed) / oneDay
                // If lastReviewed is in the future (skipped), exclude from suggestions
                if (days < 0) return@mapNotNull null
                days
            } else {
                Long.MAX_VALUE // Never reviewed
            }
            
            // Calculate priority score (higher = more urgent)
            val score = calculateReviewScore(topic, daysSinceReview)
            
            TopicSuggestion(topic, score, daysSinceReview)
        }
        .sortedByDescending { it.priorityScore }
        .take(limit)
    }
    
    /**
     * Calculate review priority score
     * Factors: days since last review, review count, difficulty, manual priority
     */
    private fun calculateReviewScore(topic: Topic, daysSinceReview: Long): Double {
        // If manual priority is set, use it directly (scaled to match typical score range)
        if (topic.manualPriority != null) {
            return topic.manualPriority.toDouble() * 10.0 // Scale 0-10 to 0-100 range
        }
        
        var score = 0.0
        
        // Days since review (more days = higher priority)
        score += daysSinceReview.coerceAtMost(30) * 2.0
        
        // Low review count = higher priority
        score += (10 - topic.reviewCount.coerceAtMost(10)) * 1.5
        
        // Higher difficulty = higher priority
        score += topic.difficulty * 1.0
        
        // Never reviewed gets high priority
        if (topic.lastReviewed == null) {
            score += 50.0
        }
        
        return score
    }
    
    /**
     * Mark topic as reviewed
     */
    fun markAsReviewed(topicId: String) {
        try {
            val topic = DataManager.getTopics().find { it.id == topicId }
            if (topic != null) {
                val updated = topic.copy(
                    lastReviewed = System.currentTimeMillis(),
                    reviewCount = topic.reviewCount + 1
                )
                DataManager.updateTopic(updated)
            } else {
                println("Warning: Topic with id $topicId not found")
            }
        } catch (e: Exception) {
            println("Error marking topic as reviewed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Skip topic for now by postponing its review priority
     */
    fun skipTopic(topicId: String) {
        try {
            val topic = DataManager.getTopics().find { it.id == topicId }
            if (topic != null) {
                // Set lastReviewed to 7 days in the future to effectively skip it
                val oneDay = 24 * 60 * 60 * 1000L
                val futureTime = System.currentTimeMillis() + (7 * oneDay)
                val updated = topic.copy(
                    lastReviewed = futureTime,
                    reviewCount = topic.reviewCount
                )
                DataManager.updateTopic(updated)
            } else {
                println("Warning: Topic with id $topicId not found")
            }
        } catch (e: Exception) {
            println("Error skipping topic: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Get topics by difficulty
     */
    fun getTopicsByDifficulty(subjectId: String? = null, difficulty: Int): List<Topic> {
        val topics = if (subjectId != null) {
            DataManager.getTopics().filter { it.subjectId == subjectId }
        } else {
            DataManager.getTopics()
        }
        
        return topics.filter { it.difficulty == difficulty }
    }
    
    data class TopicSuggestion(
        val topic: Topic,
        val priorityScore: Double,
        val daysSinceReview: Long
    )
}

