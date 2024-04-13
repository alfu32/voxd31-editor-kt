package com.voxd31.editor

import kotlin.math.ceil
import kotlin.math.floor

fun smr(fl:Float):Float{
    return (floor(1000000.0f+fl) + ceil(1000000.0f+fl))/2 - 1000000f
}
fun smr2(fl:Float):Float{
    return floor((floor(fl) + floor(1.0f+fl))/2)
}