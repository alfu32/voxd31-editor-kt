package com.voxd31.editor

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

/**
 * Calculates a rotation matrix that rotates around point 's' such that the vector 'm-s' is aligned to 'e-s'.
 *
 * @param s The pivot point for the rotation.
 * @param m The start point of the first vector before rotation.
 * @param e The end point of the second vector.
 * @return A rotation matrix that performs the specified rotation.
 */
fun calculateRotationMatrix(s: Vector3, m: Vector3, e: Vector3): Matrix4 {
    // Compute the vectors from point s to points m and e
    val v1 = Vector3(m).sub(s)
    val v2 = Vector3(e).sub(s)

    // Calculate the axis of rotation (normal to the plane defined by v1 and v2)
    val rotationAxis = Vector3(v1).crs(v2).nor()

    // Calculate the angle between v1 and v2
    val angle = Math.acos(v1.nor().dot(v2.nor()).toDouble()).toFloat() * (180f / Math.PI.toFloat())

    // Create the rotation matrix around the axis by the calculated angle
    val rotationMatrix = Matrix4().setToRotation(rotationAxis, angle)

    // Translate back and forth to rotate around point 's'
    val translateToOrigin = Matrix4().setToTranslation(-s.x, -s.y, -s.z)
    val translateBack = Matrix4().setToTranslation(s.x, s.y, s.z)

    // Combined transformation: T^-1 * R * T
    return translateBack.mul(rotationMatrix).mul(translateToOrigin)
}