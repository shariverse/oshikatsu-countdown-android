package com.basekeita.oshikatsucountdown.data.impl.repository

import com.basekeita.oshikatsucountdown.data.definition.repository.PhotoRepository
import com.basekeita.oshikatsucountdown.local.PhotoLocalDataSource
import javax.inject.Inject

internal class PhotoRepositoryImpl
@Inject
constructor(
    private val photoLocalDataSource: PhotoLocalDataSource,
) : PhotoRepository {
    override suspend fun import(sourceUri: String): String? = photoLocalDataSource.import(sourceUri)

    override suspend fun delete(path: String) {
        photoLocalDataSource.delete(path)
    }
}
