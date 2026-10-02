package com.overlay.codm

data class PlayerEntity(
    val address:   Long,
    val location:  Vec3,
    val headPos:   Vec3,
    val bones:     Map<Int, Vec3>,
    val teamId:    Int,
    val health:    Float,
    val maxHealth: Float,
    val shield:    Float,
    val maxShield: Float,
    val name:      String,
    val distance:  Float
)

data class CameraState(
    val location: Vec3,
    val pitch:    Float,
    val yaw:      Float,
    val fov:      Float
)

class EntityScanner(private val mem: MemoryReader) {

    private val base get() = mem.getBase()

    fun getCameraState(): CameraState {
        val lp          = mem.readLong(base + Offsets.LOCAL_PLAYER)
        val pc          = mem.readLong(lp + Offsets.PLAYER_CONTROLLER)
        val cm          = mem.readLong(pc + Offsets.CAMERA_MANAGER)
        val loc         = mem.readVec3(cm + Offsets.POV_LOCATION)
        val (pitch, yaw) = mem.readFloat2(cm + Offsets.POV_ROTATION)
        val fov         = mem.readFloat(cm + Offsets.POV_FOV).coerceIn(60f, 120f)
        return CameraState(loc, pitch, yaw, fov)
    }

    fun getLocalTeam(): Int {
        val lp = mem.readLong(base + Offsets.LOCAL_PLAYER)
        val ps = mem.readLong(lp + Offsets.PLAYER_STATE)
        return if (ps > 0) mem.readInt(ps + Offsets.TEAM_ID) else -1
    }

    fun scanEntities(camPos: Vec3, maxDistance: Float = 30000f): List<PlayerEntity> {
        val list      = mem.readLong(base + Offsets.ENTITY_LIST)
        val count     = mem.readInt(base + Offsets.ENTITY_COUNT).coerceIn(0, 128)
        val localTeam = getLocalTeam()
        val results   = mutableListOf<PlayerEntity>()

        for (i in 0 until count) {
            val actor = mem.readLong(list + i * 8L)
            if (actor <= 0L) continue
            val root  = mem.readLong(actor + Offsets.ROOT_COMPONENT)
            if (root  <= 0L) continue
            val loc   = mem.readVec3(root + Offsets.RELATIVE_LOCATION)
            val dist  = camPos.distanceTo(loc)
            if (dist > maxDistance) continue
            val hp    = mem.readFloat(actor + Offsets.HEALTH)
            val maxHp = mem.readFloat(actor + Offsets.MAX_HEALTH)
            if (hp <= 0f || maxHp <= 0f) continue
            val shield    = mem.readFloat(actor + Offsets.SHIELD)
            val maxShield = mem.readFloat(actor + Offsets.MAX_SHIELD)
            val ps        = mem.readLong(actor + Offsets.PLAYER_STATE)
            val teamId    = if (ps > 0) mem.readInt(ps + Offsets.TEAM_ID) else -1
            if (teamId == localTeam) continue
            val name      = if (ps > 0) mem.readFString(ps + Offsets.PLAYER_NAME) else ""
            val bones     = readBones(actor)
            val head      = bones[Offsets.BONE_HEAD] ?: Vec3(loc.x, loc.y, loc.z + 70f)
            results.add(PlayerEntity(actor, loc, head, bones, teamId,
                hp, maxHp, shield, maxShield, name, dist))
        }
        return results.sortedBy { it.distance }
    }

    private fun readBones(actor: Long): Map<Int, Vec3> {
        val arr = mem.readLong(actor + Offsets.BONE_ARRAY)
        if (arr <= 0L) return emptyMap()
        val indices = listOf(
            Offsets.BONE_HEAD, Offsets.BONE_NECK, Offsets.BONE_CHEST, Offsets.BONE_PELVIS,
            Offsets.BONE_L_SHOULDER, Offsets.BONE_R_SHOULDER,
            Offsets.BONE_L_ELBOW,    Offsets.BONE_R_ELBOW,
            Offsets.BONE_L_HAND,     Offsets.BONE_R_HAND,
            Offsets.BONE_L_KNEE,     Offsets.BONE_R_KNEE,
            Offsets.BONE_L_FOOT,     Offsets.BONE_R_FOOT
        )
        val result = mutableMapOf<Int, Vec3>()
        for (i in indices) {
            val v = mem.readVec3(arr + i * 48L)
            if (v.x != 0f || v.y != 0f || v.z != 0f) result[i] = v
        }
        return result
    }
}
