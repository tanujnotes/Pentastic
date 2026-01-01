package app.pentastic.db

import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PentasticDatabaseConstructor : RoomDatabaseConstructor<PentasticDatabase> {
    override fun initialize(): PentasticDatabase
}