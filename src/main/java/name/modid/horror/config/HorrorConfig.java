package name.modid.horror.config;

public class HorrorConfig {

    public Countdown countdown = new Countdown();
    public Sounds sounds = new Sounds();
    public Overlay overlay = new Overlay();

    public static class Countdown {
        public int startDistance = 100;
        public int runThreshold = 35;

        public int startInterval = 20;
        public int minimumInterval = 1;

        public double speedupMultiplier = 0.97;
    }

    public static class Sounds {
        public String tickSound = "minecraft:block.note_block.basedrum";
        public String endSound = "minecraft:entity.wither.spawn";

        public boolean tickSoundEnabled = true;
        public boolean endSoundEnabled = true;
    }

    public static class Overlay {
        public String runText = "RUN";
        public boolean showDistanceSuffix = true;
        public String distanceSuffix = "m";
    }
}