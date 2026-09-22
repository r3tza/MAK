package dev.retza.mak.domain

fun uniqueCollisionCount(collisions: Collection<Collision>): Int =
    collisions.map(::collisionKey).toSet().size

fun collisionKey(collision: Collision): String {
    val occurrenceIds = listOf(collision.first.id, collision.second.id).sorted()
    return listOf(
        occurrenceIds[0],
        occurrenceIds[1],
        collision.overlapStart,
        collision.overlapEnd
    ).joinToString("|")
}
