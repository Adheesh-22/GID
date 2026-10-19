package com.adheesh.gid.feature.tasks.data

import com.adheesh.gid.core.database.TaskDao
import com.adheesh.gid.core.database.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepositoryImpl(private val dao: TaskDao) : TaskRepository {
    override fun observeTasksForDay(epochDay: Long)                      = dao.observeTasksForDay(epochDay)
    override fun observeTasksForRange(fromDay: Long, toDay: Long)        = dao.observeTasksForRange(fromDay, toDay)
    override fun observeTask(id: String)                                  = dao.observeTask(id)
    override fun observeTaskDays(fromDay: Long, toDay: Long)              = dao.observeTaskDays(fromDay, toDay)
    override suspend fun getTasksForDay(epochDay: Long)                   = dao.getTasksForDay(epochDay)
    override suspend fun isCarriedForward(id: String)                     = dao.countCarriedFrom(id) > 0
    override suspend fun getTask(id: String)                              = dao.getTask(id)
    override suspend fun save(task: TaskEntity)                           = dao.upsert(task)
    override suspend fun saveAll(tasks: List<TaskEntity>)                 = dao.upsertAll(tasks)
    override suspend fun delete(id: String)                               = dao.deleteById(id)
    override suspend fun deleteAll()                                      = dao.deleteAll()
}
