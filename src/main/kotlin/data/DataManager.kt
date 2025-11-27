package com.smartstudy.data

import com.smartstudy.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

object DataManager {
    private val dataDir = File("data")
    private val subjectsFile = File(dataDir, "subjects.json")
    private val studySessionsFile = File(dataDir, "study_sessions.json")
    private val gradesFile = File(dataDir, "grades.json")
    private val scheduleItemsFile = File(dataDir, "schedule_items.json")
    private val topicsFile = File(dataDir, "topics.json")
    private val alertsFile = File(dataDir, "alerts.json")
    private val userProfileFile = File(dataDir, "user_profile.json")
    private val sessionFile = File(dataDir, "user_session.json")
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    // In-memory data storage
    private var subjects: MutableList<Subject> = mutableListOf()
    private var studySessions: MutableList<StudySession> = mutableListOf()
    private var grades: MutableList<Grade> = mutableListOf()
    private var scheduleItems: MutableList<ScheduleItem> = mutableListOf()
    private var topics: MutableList<Topic> = mutableListOf()
    private var alerts: MutableList<PerformanceAlert> = mutableListOf()
    private var userProfile: UserProfile? = null
    private var userSession: UserSession = UserSession()
    
    init {
        ensureDataDirectory()
        loadAllData()
    }
    
    private fun ensureDataDirectory() {
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
    }
    
    // Load all data from JSON files
    fun loadAllData() {
        subjects = loadFromFile(subjectsFile, emptyList())
        studySessions = loadFromFile(studySessionsFile, emptyList())
        grades = loadFromFile(gradesFile, emptyList())
        scheduleItems = loadFromFile(scheduleItemsFile, emptyList())
        topics = loadFromFile(topicsFile, emptyList())
        alerts = loadFromFile(alertsFile, emptyList())
        userProfile = loadSingle(userProfileFile)
        userSession = loadSingle(sessionFile) ?: UserSession()
    }
    
    private inline fun <reified T> loadFromFile(file: File, default: List<T>): MutableList<T> {
        return if (file.exists() && file.length() > 0) {
            try {
                json.decodeFromString<List<T>>(file.readText()).toMutableList()
            } catch (e: Exception) {
                println("Error loading ${file.name}: ${e.message}")
                default.toMutableList()
            }
        } else {
            default.toMutableList()
        }
    }
    
    private inline fun <reified T> loadSingle(file: File): T? {
        return if (file.exists() && file.length() > 0) {
            try {
                json.decodeFromString<T>(file.readText())
            } catch (e: Exception) {
                println("Error loading ${file.name}: ${e.message}")
                null
            }
        } else null
    }
    
    // Save all data to JSON files
    fun saveAllData() {
        saveToFile(subjectsFile, subjects)
        saveToFile(studySessionsFile, studySessions)
        saveToFile(gradesFile, grades)
        saveToFile(scheduleItemsFile, scheduleItems)
        saveToFile(topicsFile, topics)
        saveToFile(alertsFile, alerts)
        saveSingle(userProfileFile, userProfile)
        saveSingle(sessionFile, userSession)
    }
    
    private inline fun <reified T> saveToFile(file: File, data: List<T>) {
        try {
            file.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            println("Error saving ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private inline fun <reified T> saveSingle(file: File, data: T?) {
        try {
            if (data == null) {
                if (file.exists()) file.delete()
            } else {
                file.writeText(json.encodeToString(data))
            }
        } catch (e: Exception) {
            println("Error saving ${file.name}: ${e.message}")
        }
    }
    
    // Subjects
    fun getSubjects(): List<Subject> = subjects.toList()
    fun addSubject(subject: Subject) {
        subjects.add(subject)
        saveToFile(subjectsFile, subjects)
    }
    fun updateSubject(subject: Subject) {
        val index = subjects.indexOfFirst { it.id == subject.id }
        if (index >= 0) {
            subjects[index] = subject
            saveToFile(subjectsFile, subjects)
        }
    }
    fun deleteSubject(id: String) {
        subjects.removeAll { it.id == id }
        saveToFile(subjectsFile, subjects)
    }
    
    // Study Sessions
    fun getStudySessions(): List<StudySession> = studySessions.toList()
    fun addStudySession(session: StudySession) {
        studySessions.add(session)
        saveToFile(studySessionsFile, studySessions)
    }
    fun updateStudySession(session: StudySession) {
        val index = studySessions.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            studySessions[index] = session
            saveToFile(studySessionsFile, studySessions)
        }
    }
    fun deleteStudySession(id: String) {
        studySessions.removeAll { it.id == id }
        saveToFile(studySessionsFile, studySessions)
    }
    
    // Grades
    fun getGrades(): List<Grade> = grades.toList()
    fun addGrade(grade: Grade) {
        grades.add(grade)
        saveToFile(gradesFile, grades)
    }
    fun updateGrade(grade: Grade) {
        val index = grades.indexOfFirst { it.id == grade.id }
        if (index >= 0) {
            grades[index] = grade
            saveToFile(gradesFile, grades)
        }
    }
    fun deleteGrade(id: String) {
        grades.removeAll { it.id == id }
        saveToFile(gradesFile, grades)
    }
    
    // Schedule Items
    fun getScheduleItems(): List<ScheduleItem> = scheduleItems.toList()
    fun addScheduleItem(item: ScheduleItem) {
        scheduleItems.add(item)
        saveToFile(scheduleItemsFile, scheduleItems)
    }
    fun updateScheduleItem(item: ScheduleItem) {
        val index = scheduleItems.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            scheduleItems[index] = item
            saveToFile(scheduleItemsFile, scheduleItems)
        }
    }
    fun deleteScheduleItem(id: String) {
        scheduleItems.removeAll { it.id == id }
        saveToFile(scheduleItemsFile, scheduleItems)
    }
    
    // Topics
    fun getTopics(): List<Topic> = topics.toList()
    fun addTopic(topic: Topic) {
        topics.add(topic)
        saveToFile(topicsFile, topics)
    }
    fun updateTopic(topic: Topic) {
        val index = topics.indexOfFirst { it.id == topic.id }
        if (index >= 0) {
            topics[index] = topic
            saveToFile(topicsFile, topics)
        }
    }
    fun deleteTopic(id: String) {
        topics.removeAll { it.id == id }
        saveToFile(topicsFile, topics)
    }
    
    // Alerts
    fun getAlerts(): List<PerformanceAlert> = alerts.toList()
    fun addAlert(alert: PerformanceAlert) {
        alerts.add(alert)
        saveToFile(alertsFile, alerts)
    }
    fun updateAlert(alert: PerformanceAlert) {
        val index = alerts.indexOfFirst { it.id == alert.id }
        if (index >= 0) {
            alerts[index] = alert
            saveToFile(alertsFile, alerts)
        }
    }
    fun deleteAlert(id: String) {
        alerts.removeAll { it.id == id }
        saveToFile(alertsFile, alerts)
    }
    
    // User profile & session
    fun getUserProfile(): UserProfile? = userProfile
    
    fun saveUserProfile(profile: UserProfile) {
        userProfile = profile
        saveSingle(userProfileFile, profile)
    }
    
    fun getUserSession(): UserSession = userSession
    
    fun saveUserSession(session: UserSession) {
        userSession = session
        saveSingle(sessionFile, session)
    }
    
    fun clearUserSession() {
        userSession = UserSession()
        saveSingle(sessionFile, userSession)
    }
}

