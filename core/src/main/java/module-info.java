module core {
    requires it.unimi.dsi.fastutil;
    requires org.apache.commons.io;
    requires org.jctools.core;
    requires org.jetbrains.annotations;
    requires org.tinylog.api;
    requires org.apache.commons.collections4;

    exports io.github.arkosammy12.jemu.core.common;
    exports io.github.arkosammy12.jemu.core.rca.cosmacvip;
    exports io.github.arkosammy12.jemu.core.rca.studioii;
    exports io.github.arkosammy12.jemu.core.cpu;
    exports io.github.arkosammy12.jemu.core.drivers;
    exports io.github.arkosammy12.jemu.core.exceptions;
    exports io.github.arkosammy12.jemu.core.nintendo.gameboy;
    exports io.github.arkosammy12.jemu.core.nintendo.gameboycolor;
    exports io.github.arkosammy12.jemu.core.nintendo.nes;
    exports io.github.arkosammy12.jemu.core.nintendo.nes.ines;
    exports io.github.arkosammy12.jemu.core.nintendo.nes.mappers;
    exports io.github.arkosammy12.jemu.core.nintendo.gameboy.mbcs;
    exports io.github.arkosammy12.jemu.core.rca;

}