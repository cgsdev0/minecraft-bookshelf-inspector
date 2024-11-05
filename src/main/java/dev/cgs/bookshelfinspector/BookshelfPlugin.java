package dev.cgs.bookshelfinspector;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ChiseledBookshelfInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.joml.Vector2d;

import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.item.WrittenBookItem;

import static net.kyori.adventure.text.Component.text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.Map;

@DefaultQualifier(NonNull.class)
public final class BookshelfPlugin extends JavaPlugin implements Listener {

  public static Vector2d project(Vector v, BlockFace face) {
    switch(face) {
      case SOUTH:
        return new Vector2d(-v.getX(), 1 + v.getY());
      case NORTH:
        return new Vector2d(1 + v.getX(), 1 + v.getY());
      case EAST:
        return new Vector2d(1 + v.getZ(), 1 + v.getY());
      case WEST:
        return new Vector2d(-v.getZ(), 1 + v.getY());
      default:
        return null;
    }
  }

  public static int slot(Vector2d v) {
    int result = 0;
    if (v.y > 0.5) result += 3;
    if (v.x > 0.33) result += 1;
    if (v.x > 0.66) result += 1;
    return result;
  }

  @Override
  public void onDisable() {
  }

  @Override
  public void onEnable() {
    this.getServer().getPluginManager().registerEvents(this, this);

    BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
              for (Player p : getServer().getOnlinePlayers()) {
                RayTraceResult result = p.rayTraceBlocks(4.0);
                if (result == null) continue;
                Block b = result.getHitBlock();
                if (b == null) continue;
                if (b.getType() != Material.CHISELED_BOOKSHELF) continue;
                BlockFace face = result.getHitBlockFace();
                if (face == BlockFace.DOWN || face == BlockFace.UP) continue;
                ChiseledBookshelf state = (ChiseledBookshelf)b.getState();
                org.bukkit.block.data.type.ChiseledBookshelf data = (org.bukkit.block.data.type.ChiseledBookshelf)b.getBlockData();
                if (face != data.getFacing()) continue;
                Vector origin = b.getLocation().toVector();
                Vector pos = result.getHitPosition();
                Vector diff = pos.multiply(-1.0).add(origin);
                int slot = BookshelfPlugin.slot(BookshelfPlugin.project(diff, face));
                if (!data.isSlotOccupied(slot)) continue;
                ChiseledBookshelfInventory inv = state.getInventory();
                ItemStack item = inv.getItem(slot);
                switch(item.getType()) {
                  case WRITTEN_BOOK:
                    BookMeta book = (BookMeta)item.getItemMeta();
                    Component res = text("Written Book");
                    if (book.hasTitle()) {
                      res = text('"' + book.getTitle() + '"', NamedTextColor.AQUA);
                    }
                    if (book.hasAuthor()) {
                      res = res.append(text(" by ", NamedTextColor.GRAY).append(text(book.getAuthor(), NamedTextColor.LIGHT_PURPLE)));
                    }
                    if (book.getGeneration() == BookMeta.Generation.ORIGINAL) {
                      res = res.append(text(" (original)", NamedTextColor.GRAY));
                    }
                    p.sendActionBar(res);
                    break;
                  case WRITABLE_BOOK:
                    p.sendActionBar(
                      text("Book and Quill", NamedTextColor.WHITE)
                    );
                    break;
                  case ENCHANTED_BOOK:
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta)item.getItemMeta();
                    Component ench = text("Enchanted");
                    int max = 0;
                    for (Map.Entry<Enchantment, Integer> entry : meta.getStoredEnchants().entrySet()) {
                      Enchantment e = entry.getKey();
                      if (entry.getValue() > max) {
                        ench = e.displayName(entry.getValue()).color(NamedTextColor.GOLD);
                        max = entry.getValue();
                      }
                    }

                    ench = ench.append(
                      text(" Book", NamedTextColor.GRAY)
                    );

                    int len = meta.getStoredEnchants().size();
                    if (len > 1) {
                      ench = ench.append(text(" (+" + (len - 1) + " enchantment(s))", NamedTextColor.GRAY));
                    }
                    p.sendActionBar(ench);
                    break;
                  case BOOK:
                    p.sendActionBar(
                      text("Book")
                    );
                    break;
                  default:
                    continue;
                }
              }
            }
        }, 0L, 1L);
  }
}
