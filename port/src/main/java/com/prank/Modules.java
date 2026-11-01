package com.prank;

import java.util.ArrayList;
import java.util.List;

public class Modules {
    public static final List<Module> ALL = new ArrayList<>();
    public static final String[] CATS = {"ESP", "World", "HUD", "Misc", "Visual", "Move", "Combat", "Player", "Extra", "Sound", "GUI"};

    private static Module m(String n, String d, String c, boolean on) {
        Module x = new Module(n, d, c, on);
        ALL.add(x);
        return x;
    }

    // ================= ESP =================
    public static final Module TRACERS = m("Tracers", "Lines to players", "ESP", false).color(0xFFA78BFA);
    public static final Module TWO_D = m("2D ESP", "Boxes on screen", "ESP", false).color(0xFFA78BFA);
    public static final Module THREE_D = m("3D ESP", "Boxes in the world", "ESP", true).color(0xFFA78BFA);
    public static final Module SKELETON = m("Skeleton", "Simple bone lines", "ESP", false).color(0xFFFFFFFF);
    public static final Module CHAMS = m("Chams", "Filled see-through", "ESP", false).color(0xFFA78BFA);
    public static final Module GLOW = m("Glow ESP", "Glowing outlines", "ESP", false).color(0xFF55FFFF);
    public static final Module HEALTH = m("Health ESP", "Health bar on box", "ESP", false);
    public static final Module TARGETESP = m("Target ESP", "Glowing target marker", "ESP", false)
        .color(0xFFFFFFFF).num("Style", 1, 1, 3, 1).bool("Crosshair Only", true)
        .num("Size", 1.0, 0.5, 2.5, 0.1).num("Speed", 1.0, 0.2, 4.0, 0.2);
    public static final Module TRAJECTORIES = m("Trajectories", "Pearl/bow landing path", "ESP", false).color(0xFF2EC4FF);
    public static final Module PROJECTILE_ESP = m("Projectile ESP", "Arrows, pearls...", "ESP", false).color(0xFFFFE066);

    // ================= World =================
    public static final Module ITEM_ESP = m("Item ESP", "Dropped items", "World", false).color(0xFFFFE066);
    public static final Module STORAGE_ESP = m("Storage ESP", "Chests, barrels...", "World", false).color(0xFFFF9F43);
    public static final Module MOB_ESP = m("Mob ESP", "Mobs through walls", "World", false).color(0xFF55FF55);
    public static final Module ANTI_INVIS = m("Anti-Invisible", "Reveals invisible", "World", false);
    public static final Module ORE_ESP = m("Ore ESP", "Ores nearby", "World", false).num("Radius", 16, 4, 24, 2);
    public static final Module HITBOXES = m("Hitboxes", "All entity boxes", "World", false).color(0xFFFFFFFF);
    public static final Module TIMECHANGER = m("Time Changer", "Set the world time", "World", false)
        .num("Time", 6000, 0, 24000, 1000);
    public static final Module NOWEATHER = m("No Weather", "Always clear sky", "World", false);
    public static final Module AMETHYST = m("Amethyst Finder", "Finds grown amethyst", "World", false)
        .color(0xFFB388FF).movable()
        .num("Chunks", 8, 2, 16, 1).num("Min Clusters", 6, 1, 40, 1).num("Max Targets", 3, 1, 10, 1)
        .bool("Tracer", true).bool("Boxes", true);

    // ================= HUD =================
    public static final Module NAMETAGS = m("Nametags", "Name, gear, ping", "HUD", true).color(0xFFFFFFFF)
        .num("Scale", 1.0, 0.4, 2.5, 0.1)
        .bool("Background", true)
        .num("Max Distance", 64, 16, 256, 16)
        .bool("Hide Vanilla", true)
        .num("Y Offset", 0.0, -1.5, 2.0, 0.1)
        .bool("Items", true)
        .bool("Held Items", true)
        .bool("Durability", true)
        .bool("Health", true)
        .bool("Ping", true)
        .bool("Distance", true);
    public static final Module ARROWS = m("Arrows", "Players off-screen", "HUD", false).color(0xFFFF4D6D)
        .num("Radius", 70, 30, 150, 10);
    public static final Module RADAR = m("Radar", "Nearby entities map", "HUD", false).color(0xFFFF4D6D)
        .movable().num("Range", 40, 10, 100, 10).num("Scale", 1.0, 0.5, 2.0, 0.1);
    public static final Module TARGET_HUD = m("Target HUD", "Animated target info", "HUD", false)
        .color(0xFF2EC4FF).movable()
        .num("Scale", 1.0, 0.5, 2.0, 0.1).bool("Armor", true).bool("Background", true)
        .bool("Animate", true).num("Smooth", 1.0, 0.3, 3.0, 0.1);
    public static final Module INFO_HUD = m("Info HUD", "XYZ, FPS, ping", "HUD", false)
        .color(0xFFFFFFFF).movable().num("Scale", 1.0, 0.5, 2.0, 0.1).bool("Background", true);
    public static final Module PLAYER_LIST = m("Player List", "Nearby players", "HUD", false)
        .color(0xFFFFFFFF).movable().num("Scale", 1.0, 0.5, 2.0, 0.1).bool("Background", true)
        .num("Count", 6, 1, 12, 1);
    public static final Module ARRAYLIST = m("Arraylist", "Enabled modules", "HUD", true)
        .movable().num("Scale", 1.0, 0.5, 2.0, 0.1).bool("Rainbow", true);
    public static final Module KEYSTROKES = m("Keystrokes", "WASD + mouse keys", "HUD", false)
        .color(0xFF2EC4FF).movable().num("Scale", 1.0, 0.5, 2.0, 0.1);
    public static final Module ARMOR_HUD = m("Armor HUD", "Your gear + durability", "HUD", false)
        .movable().num("Scale", 1.0, 0.5, 2.0, 0.1);
    public static final Module EFFECTS_HUD = m("Effects HUD", "Potion timers", "HUD", false)
        .movable().num("Scale", 1.0, 0.5, 2.0, 0.1);
    public static final Module HITMARKER = m("Hit Marker", "Flash when you hit", "HUD", true).color(0xFFFF4D6D);
    public static final Module KEYBINDS_HUD = m("Keybinds HUD", "Your key binds", "HUD", false)
        .movable().num("Scale", 1.0, 0.5, 2.0, 0.1);

    // ================= Misc =================
    public static final Module BREADCRUMBS = m("Breadcrumbs", "Movement trails", "Visual", false).color(0xFFA78BFA)
        .num("Length", 120, 20, 400, 20);
    public static final Module CHINA_HAT = m("China Hat", "Hat on players", "Visual", false).color(0xFFA78BFA);
    public static final Module FREECAM = m("Freecam", "Fly cam, short reach", "Misc", false)
        .num("Speed", 12, 2, 40, 2).num("Reach", 3, 1, 6, 0.5);
    public static final Module FULLBRIGHT = m("Fullbright", "See in the dark", "Misc", false);
    public static final Module ZOOM = m("Zoom", "Lower FOV", "Misc", false).num("FOV", 30, 30, 90, 5);
    public static final Module NOHURTCAM = m("No Hurt Cam", "No screen shake", "Misc", false);
    public static final Module TRAIL = m("Trail", "Particles behind you", "Visual", false)
        .color(0xFF2EC4FF).num("Type", 1, 1, 12, 1).num("Amount", 2, 1, 8, 1);
    public static final Module HITPARTICLES = m("Hit Particles", "Burst when you hit", "Visual", false)
        .color(0xFF2EC4FF).num("Type", 4, 1, 12, 1).num("Count", 8, 1, 30, 1);
    public static final Module CUSTOMSWING = m("Custom Swing", "Hand swing styles", "Misc", false)
        .num("Speed", 6, 2, 20, 1).num("Style", 1, 1, 4, 1).bool("No Equip Anim", true)
        .num("Scale", 1.0, 0.5, 1.5, 0.1).num("Y Offset", 0.0, -0.5, 0.5, 0.1);
    public static final Module CROSSHAIR = m("Crosshair", "Custom crosshair", "Misc", false)
        .color(0xFF2EC4FF).num("Size", 5, 1, 12, 1).num("Gap", 3, 0, 10, 1)
        .num("Thickness", 1, 1, 3, 1).bool("Dot", true).bool("Cooldown", true);
    public static final Module NOOVERLAY = m("No Overlay", "No fire overlay", "Misc", false);
    public static final Module WAYPOINTS = m("Waypoints", "Markers + death points", "Misc", true)
        .color(0xFFFFE066).num("Max Distance", 5000, 100, 20000, 100).bool("Arrows", true)
        .bool("Beams", true).bool("Death Points", true).num("Scale", 1.0, 0.5, 2.0, 0.1)
        .bool("Remove Nearest", false).bool("Clear All", false);
    public static final Module AIRJUMP = m("AirJump", "Extra jumps in the air", "Misc", false).num("Jumps", 1, 1, 5, 1);
    public static final Module FASTCLIMB = m("FastClimb", "Faster ladders", "Misc", false).num("Speed", 0.3, 0.1, 0.6, 0.05);
    public static final Module FASTFALL = m("FastFall", "Sneak to drop fast", "Misc", false).num("Speed", 0.8, 0.3, 2.0, 0.1);
    public static final Module ANTIBLIND = m("AntiBlind", "No blindness/nausea", "Misc", false);

    // ================= Visual =================
    public static final Module CUSTOMSKY = m("Custom Sky", "Sky + cloud color", "Visual", false)
        .color(0xFF2EC4FF).bool("Clouds", true).num("Brightness", 1.0, 0.2, 1.5, 0.1);
    public static final Module PARTICLEAURA = m("Particle Aura", "Orbiting particles", "Visual", false)
        .color(0xFF2EC4FF).num("Type", 7, 1, 12, 1).num("Amount", 4, 1, 12, 1)
        .num("Radius", 1.0, 0.4, 3.0, 0.1).num("Speed", 1.0, 0.2, 4.0, 0.2);
    public static final Module DAMAGEPOPUPS = m("Damage Popups", "Floating damage numbers", "Visual", false)
        .color(0xFFFF4D6D).num("Scale", 1.0, 0.5, 2.5, 0.1).num("Duration", 1.2, 0.5, 3.0, 0.1);
    public static final Module BLOCKOUTLINE = m("Block Outline", "Colored target outline", "Visual", false)
        .color(0xFF2EC4FF);
    public static final Module DAMAGEFLASH = m("Damage Flash", "Red edges when hurt", "Visual", false)
        .color(0xFFFF4D6D).num("Duration", 0.6, 0.2, 2.0, 0.1).bool("Low Health", true).num("Threshold", 6, 2, 12, 1);
    public static final Module COMPASS = m("Compass", "Direction strip", "Visual", false)
        .color(0xFF2EC4FF).movable().num("Scale", 1.0, 0.5, 2.0, 0.1);

    // ================= Move =================
    public static final Module FLY = m("Fly", "Creative-style flight", "Move", false).num("Speed", 0.6, 0.1, 2.0, 0.1)
        .num("Vertical", 0.5, 0.1, 2.0, 0.1).bool("Anti Kick", true);
    public static final Module SPEED = m("Speed", "Faster walking", "Move", false).num("Mult", 1.5, 1.0, 4.0, 0.5)
        .bool("Ground Only", false);
    public static final Module GLIDE = m("Glide", "Slow falling", "Move", false).num("Fall Speed", 0.1, 0.02, 0.3, 0.02);
    public static final Module SPIDER = m("Spider", "Climb walls", "Move", false).num("Speed", 0.2, 0.1, 0.5, 0.05);
    public static final Module BUNNYHOP = m("BunnyHop", "Auto jump", "Move", false).num("Height", 0.42, 0.2, 0.8, 0.02);
    public static final Module NOFALL = m("NoFall", "No fall damage", "Move", false).num("Distance", 2.5, 1.0, 10.0, 0.5);
    public static final Module AUTOSPRINT = m("AutoSprint", "Always sprint", "Move", false);
    public static final Module TWERK = m("Twerk", "Spam sneak (prank)", "Move", false);
    public static final Module HIGHJUMP = m("HighJump", "Jump higher", "Move", false).num("Height", 0.8, 0.5, 2.0, 0.1);
    public static final Module JESUS = m("Jesus", "Float on water", "Move", false);
    public static final Module AUTOJUMP = m("AutoJump", "Hop over blocks", "Move", false);
    public static final Module LONGJUMP = m("LongJump", "Jump farther", "Move", false).num("Boost", 1.6, 1.1, 3.0, 0.1);

    // ================= Combat =================
    public static final Module KILLAURA = m("KillAura", "Hits nearby targets", "Combat", false)
        .bool("Players", true).bool("Mobs", false).num("Range", 3.5, 1, 6, 0.5).bool("Rotate", false)
        .num("Priority", 1, 1, 2, 1).num("Cooldown", 0.95, 0.5, 1.0, 0.05);
    public static final Module TRIGGERBOT = m("TriggerBot", "Hits what you aim at", "Combat", false)
        .bool("Players", true).bool("Mobs", false).num("Cooldown", 0.95, 0.5, 1.0, 0.05);
    public static final Module AUTOTOTEM = m("AutoTotem", "Totem to offhand", "Combat", false)
        .num("Delay", 8, 0, 20, 1).num("Health", 20, 2, 20, 1);
    public static final Module HITBOX = m("HitBox", "Bigger player hitbox", "Combat", false)
        .num("Size", 0.3, 0.0, 1.0, 0.1);
    public static final Module CRITICALS = m("Criticals", "Crit on every hit", "Combat", false);
    public static final Module AIMASSIST = m("AimAssist", "Nudges aim to players", "Combat", false)
        .num("Range", 5, 2, 8, 0.5).num("Speed", 6, 1, 20, 1).num("FOV", 90, 10, 180, 10).bool("Click Only", true);
    public static final Module AUTOCLICKER = m("AutoClicker", "Hold to hit", "Combat", false);
    public static final Module NOFRIENDDAMAGE = m("NoFriendDamage", "Never hit friends", "Combat", true);
    public static final Module MIDDLEFRIEND = m("MiddleClickFriend", "Middle click = friend", "Combat", true);
    public static final Module KILLEFFECT = m("Kill Effect", "Effect when you kill", "Combat", true)
        .color(0xFF2EC4FF).num("Type", 1, 1, 5, 1).num("Count", 24, 4, 80, 4).bool("Message", true);
    public static final Module POPALERT = m("Pop Alert", "Totem pop notices", "Combat", false);
    public static final Module AUTOGG = m("AutoGG", "Says gg after a kill", "Combat", false)
        .num("Style", 1, 1, 4, 1);

    // ================= Player =================
    public static final Module AUTOTOOL = m("AutoTool", "Best tool to mine", "Player", false);
    public static final Module AUTORESPAWN = m("AutoRespawn", "Respawn instantly", "Player", false);
    public static final Module SCAFFOLD = m("Scaffold", "Bridge under you", "Player", false);
    public static final Module CHESTSTEALER = m("ChestStealer", "Takes all items", "Player", false)
        .num("Delay", 2, 0, 10, 1);
    public static final Module ANTIAFK = m("AntiAFK", "Hops now and then", "Player", false).num("Interval", 20, 5, 120, 5);
    public static final Module DRIPTRAP = m("DripTrap", "Place + drop dripstone", "Player", false)
        .num("Delay", 10, 0, 60, 5).bool("Underside", true);
    public static final Module DRIPRELEASE = m("DripRelease", "Drop when target under", "Player", false)
        .bool("Include Self", true);
    public static final Module NOPUSH = m("NoPush", "Entities, blocks, liquids", "Player", false)
        .bool("Entities", true).bool("Blocks", true).bool("Liquids", true);
    public static final Module INVENTORYMOVE = m("InventoryMove", "Walk in inventory", "Player", false);
    public static final Module NOJUMPDELAY = m("NoJumpDelay", "Jump instantly", "Player", false);
    public static final Module ELYTRASWAP = m("ElytraSwap", "Chestplate <-> elytra", "Player", false);
    public static final Module NAMEPROTECT = m("NameProtect", "Hides your real name", "Player", false)
        .num("Style", 1, 1, 6, 1);

    // ================= Extra =================
    public static final Module FASTPLACE = m("FastPlace", "No block place delay", "Extra", false)
        .num("Delay", 1, 1, 4, 1);
    public static final Module PARKOUR = m("Parkour", "Jump at edges", "Extra", false);
    public static final Module AUTOWALK = m("AutoWalk", "Holds forward", "Extra", false);
    public static final Module AUTOMINE = m("AutoMine", "Holds left click", "Extra", false);
    public static final Module AUTOARMOR = m("AutoArmor", "Wears best armor", "Extra", false);
    public static final Module AUTOLOG = m("AutoLog", "Quit at low health", "Extra", false)
        .num("Health", 6, 1, 19, 1);
    public static final Module ANTIVOID = m("AntiVoid", "Bounce out of the void", "Extra", false)
        .num("Height", 3, 0, 10, 1);
    public static final Module DURABILITY = m("DurabilityAlert", "Warns on low armor", "Extra", false)
        .num("Percent", 15, 5, 50, 5);
    public static final Module WAYPOINT_ADD = m("Mark Waypoint", "Save this spot", "Extra", false);
    public static final Module AUTOWATER = m("AutoWater", "Water washes levers away", "Extra", false)
        .bool("Pick Up Water", true).bool("Place Block After", true).num("Delay", 2, 0, 10, 1);
    public static final Module SAFEWALK = m("SafeWalk", "Don't walk off edges", "Extra", false);
    public static final Module AUTOREFILL = m("AutoRefill", "Refills your hotbar", "Extra", false);

    // ================= Sound =================
    public static final Module CRITSOUND = m("Crit Sound", "Custom crit sound", "Sound", true)
        .num("Volume", 1.0, 0.1, 2.0, 0.1).num("Pitch", 1.0, 0.5, 2.0, 0.1);
    public static final Module HITSOUND = m("Hit Sound", "Custom hit sound", "Sound", false)
        .num("Volume", 1.0, 0.1, 2.0, 0.1).num("Pitch", 1.0, 0.5, 2.0, 0.1);
    public static final Module DRIPIMPACT = m("Drip Impact", "Sound when dripstone hits", "Sound", true)
        .num("Volume", 1.2, 0.1, 2.0, 0.1).num("Pitch", 1.0, 0.5, 2.0, 0.1);
    public static final Module GUISOUNDS = m("GUI Sounds", "Click on module toggle", "Sound", true)
        .num("Volume", 0.6, 0.1, 2.0, 0.1);
    public static final Module KILLSOUND = m("Kill Sound", "Custom kill sound", "Sound", true)
        .num("Volume", 1.2, 0.1, 2.0, 0.1).num("Pitch", 1.0, 0.5, 2.0, 0.1);

    // ================= GUI =================
    public static final Module ANIMATIONS = m("Animations", "GUI animations", "GUI", true)
        .num("Speed", 1.0, 0.5, 3.0, 0.5);
    public static final Module DARKMODE = m("Dark Mode", "Dark theme + accent", "GUI", false).color(0xFF2EC4FF);
    public static final Module LAYOUT = m("Dim Background", "Dim + GUI scale", "GUI", false)
        .num("Scale", 1.0, 0.7, 1.4, 0.1);
    public static final Module ELECTRICITY = m("Electricity", "Lightning in the menu", "GUI", true)
        .num("Intensity", 3, 1, 8, 1).num("Speed", 1.0, 0.5, 3.0, 0.5);
    public static final Module NOTIFICATIONS = m("Notifications", "Pop-ups on toggle", "GUI", true)
        .num("Duration", 2.6, 1.0, 8.0, 0.5).bool("Bounce", true).bool("Progress Bar", true);
    public static final Module WATERMARK = m("Watermark", "Kelo Client + FPS", "GUI", true)
        .color(0xFF2EC4FF).movable().bool("Show FPS", true);
    public static final Module ACCESS = m("Accessibility", "Motion, contrast, colors", "GUI", true)
        .bool("Reduce Motion", false).bool("High Contrast", false).bool("Colorblind Health", false)
        .num("HUD Scale", 1.0, 0.8, 2.0, 0.1).num("Panel Opacity", 0.56, 0.3, 1.0, 0.05);
    public static final Module RENDER = m("Render Settings", "Line width, rainbow, fill", "GUI", true)
        .num("Line Width", 1.5, 1.0, 5.0, 0.5).num("Rainbow Speed", 1.0, 0.2, 4.0, 0.2)
        .num("Fill Opacity", 0.33, 0.1, 0.8, 0.05);
}
