package io.github.swqxdba.bridge.codegen

import io.github.swqxdba.bridge.meta.ControllerMeta

interface IGenerator {
    fun generate(controllerMeta: List<ControllerMeta>)
}