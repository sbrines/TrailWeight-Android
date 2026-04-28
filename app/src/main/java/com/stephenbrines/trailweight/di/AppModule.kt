package com.stephenbrines.trailweight.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stephenbrines.trailweight.data.db.TrailWeightDatabase
import com.stephenbrines.trailweight.data.db.dao.GearItemDao
import com.stephenbrines.trailweight.data.db.dao.PackListDao
import com.stephenbrines.trailweight.data.db.dao.ResupplyDao
import com.stephenbrines.trailweight.data.db.dao.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrailWeightDatabase =
        Room.databaseBuilder(context, TrailWeightDatabase::class.java, "trailweight.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides fun provideGearItemDao(db: TrailWeightDatabase): GearItemDao = db.gearItemDao()
    @Provides fun provideTripDao(db: TrailWeightDatabase): TripDao = db.tripDao()
    @Provides fun providePackListDao(db: TrailWeightDatabase): PackListDao = db.packListDao()
    @Provides fun provideResupplyDao(db: TrailWeightDatabase): ResupplyDao = db.resupplyDao()
    @Provides fun provideWeightSnapshotDao(db: TrailWeightDatabase): com.stephenbrines.trailweight.data.db.dao.WeightSnapshotDao = db.weightSnapshotDao()
}
