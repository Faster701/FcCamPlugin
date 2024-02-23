package com.fastercraft.fc_cam;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FcCamPlugin extends JavaPlugin {

    private Map<String, Location> playerPositions = new HashMap<>();
    private Map<String, Integer> teleportTasks = new HashMap<>();

    @Override
    public void onEnable() {
        // Здесь можно загрузить конфигурацию, если требуется
        loadConfig();
        getLogger().info("Плагин Fc-Cam включен.");
    }

    @Override
    public void onDisable() {
        // Здесь можно сохранить конфигурацию, если требуется
        saveConfig();
        getLogger().info("Плагин Fc-Cam выключен.");
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
    }

    private void savePlayerPositions() {
        getConfig().set("playerPositions", null);
        for (Map.Entry<String, Location> entry : playerPositions.entrySet()) {
            String key = entry.getKey();
            Location loc = entry.getValue();
            getConfig().set("playerPositions." + key + ".world", loc.getWorld().getName());
            getConfig().set("playerPositions." + key + ".x", loc.getX());
            getConfig().set("playerPositions." + key + ".y", loc.getY());
            getConfig().set("playerPositions." + key + ".z", loc.getZ());
        }
        saveConfig();
    }

    private void loadPlayerPositions() {
        playerPositions.clear();
        if (getConfig().contains("playerPositions")) {
            for (String key : getConfig().getConfigurationSection("playerPositions").getKeys(false)) {
                String world = getConfig().getString("playerPositions." + key + ".world");
                double x = getConfig().getDouble("playerPositions." + key + ".x");
                double y = getConfig().getDouble("playerPositions." + key + ".y");
                double z = getConfig().getDouble("playerPositions." + key + ".z");
                Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                playerPositions.put(key, loc);
            }
        }
    }

    private void teleportToPosition(Player player, String positionName) {
        if (playerPositions.containsKey(positionName)) {
            player.teleport(playerPositions.get(positionName));
            player.sendMessage(ChatColor.GREEN + "Вы телепортированы на позицию: " + positionName);
        } else {
            player.sendMessage(ChatColor.RED + "Позиция с именем '" + positionName + "' не найдена.");
        }
    }

    private void startTeleportCycle(Player player, int delayInSeconds) {
        if (teleportTasks.containsKey(player.getName())) {
            player.sendMessage(ChatColor.RED + "Цикл телепортации уже запущен.");
            return;
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                List<String> positionNames = List.copyOf(playerPositions.keySet());
                if (!positionNames.isEmpty()) {
                    Random random = new Random();
                    String randomPosition = positionNames.get(random.nextInt(positionNames.size()));
                    teleportToPosition(player, randomPosition);
                }
            }
        };

        int taskId = task.runTaskTimer(this, 0L, delayInSeconds * 20L).getTaskId();
        teleportTasks.put(player.getName(), taskId);
        player.sendMessage(ChatColor.GREEN + "Цикл телепортации запущен с интервалом " + delayInSeconds + " секунд.");
    }

    private void stopTeleportCycle(Player player) {
        if (teleportTasks.containsKey(player.getName())) {
            Bukkit.getScheduler().cancelTask(teleportTasks.get(player.getName()));
            teleportTasks.remove(player.getName());
            player.sendMessage(ChatColor.GREEN + "Цикл телепортации остановлен.");
        } else {
            player.sendMessage(ChatColor.RED + "Цикл телепортации не найден.");
        }
    }

    private void deletePosition(Player player, String positionName) {
        if (playerPositions.containsKey(positionName)) {
            playerPositions.remove(positionName);
            savePlayerPositions();
            player.sendMessage(ChatColor.GREEN + "Позиция с именем '" + positionName + "' удалена.");
        } else {
            player.sendMessage(ChatColor.RED + "Позиция с именем '" + positionName + "' не найдена.");
        }
    }

    private void listPositions(Player player) {
        player.sendMessage(ChatColor.GOLD + "Сохраненные позиции:");
        for (String positionName : playerPositions.keySet()) {
            player.sendMessage("- " + positionName);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Команды могут использовать только игроки.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Используйте /fccam help для просмотра доступных команд.");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "save":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Используйте /fccam save [Имя позиции]");
                    return true;
                }
                String positionName = args[1];
                playerPositions.put(positionName, player.getLocation());
                savePlayerPositions();
                player.sendMessage(ChatColor.GREEN + "Текущая позиция сохранена под именем: " + positionName);
                break;

            case "teleport":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Используйте /fccam teleport [Имя позиции]");
                    return true;
                }
                teleportToPosition(player, args[1]);
                break;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Используйте /fccam delete [Имя позиции]");
                    return true;
                }
                deletePosition(player, args[1]);
                break;

            case "list":
                listPositions(player);
                break;

            case "startcycle":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Используйте /fccam startcycle [Интервал в секундах]");
                    return true;
                }
                int delay = Integer.parseInt(args[1]);
                startTeleportCycle(player, delay);
                break;

            case "stopcycle":
                stopTeleportCycle(player);
                break;

            case "help":
                player.sendMessage(ChatColor.GOLD + "Команды Fc-Cam:");
                player.sendMessage("/fccam save [Имя позиции] - Сохранить текущую позицию под указанным именем.");
                player.sendMessage("/fccam teleport [Имя позиции] - Телепортироваться к сохраненной позиции.");
                player.sendMessage("/fccam delete [Имя позиции] - Удалить сохраненную позицию.");
                player.sendMessage("/fccam list - Показать список сохраненных позиций.");
                player.sendMessage("/fccam startcycle [Интервал в секундах] - Запустить цикл телепортации.");
                player.sendMessage("/fccam stopcycle - Остановить цикл телепортации.");
                break;

            default:
                player.sendMessage(ChatColor.RED + "Неизвестная команда. Используйте /fccam help для просмотра доступных команд.");
                break;
        }

        return true;
    }
}
