package surreal.backportium;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import surreal.backportium.api.enums.ModArmorMaterials;
import surreal.backportium.block.ModBlocks;
import surreal.backportium.client.ClientHandler;
import surreal.backportium.command.debug.CommandGenerate;
import surreal.backportium.enchantment.ModEnchantments;
import surreal.backportium.entity.ModEntities;
import surreal.backportium.entity.v1_13.EntityTrident;
import surreal.backportium.item.ModItems;
import surreal.backportium.network.NetworkHandler;
import surreal.backportium.potion.ModPotions;
import surreal.backportium.recipe.ModRecipes;
import surreal.backportium.sound.ModSounds;
import surreal.backportium.world.biome.ModBiomes;

@Mod(modid = Tags.MOD_ID, name = "Backportium", version = Tags.MOD_VERSION, dependencies = "after:*")
@SuppressWarnings("unused")
public class Backportium {

    public static final EnumAction SPEAR = EnumHelper.addAction("SPEAR");

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        if (FMLLaunchHandler.side().isClient()) ClientHandler.construction(event);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModBlocks.registerTiles();
        ModArmorMaterials.register();
        NetworkHandler.init();
        if (FMLLaunchHandler.side().isClient()) ClientHandler.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModItems.registerOres();
        registerDispenseBehaviours();
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        if (FMLLaunchHandler.isDeobfuscatedEnvironment()) {
            // Not so good attempt for testing world generations.
            // Imagine, it's too bad that I added a check. So, it only works in the dev environment.
            event.registerServerCommand(new CommandGenerate());
        }
    }

    public static void registerDispenseBehaviours() {
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(ModItems.TRIDENT, ((source, stack) -> {
            World world = source.getWorld();
            IBlockState state = source.getBlockState();
            int riptide = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.RIPTIDE, stack);
            if (riptide != 0 || stack.getItemDamage() == stack.getMaxDamage() - 1) return stack;
            EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
            int infinity = EnchantmentHelper.getEnchantmentLevel(Enchantments.INFINITY, stack);
            EntityTrident trident = new EntityTrident(world, source.getX() + facing.getXOffset(), source.getY() + facing.getYOffset(), source.getZ() + facing.getZOffset(), stack);
            trident.shoot(facing.getXOffset(), facing.getYOffset(), facing.getZOffset(), 0.9F, 0.25F);
            world.playSound(null, source.getX(), source.getY(), source.getZ(), ModSounds.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
            world.spawnEntity(trident);
            if (infinity != 0) {
                trident.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
                stack.attemptDamageItem(1, world.rand, null);
            }
            else trident.pickupStatus = EntityArrow.PickupStatus.ALLOWED;
            return ItemStack.EMPTY;
        }));
    }

    // Registry Events
    @SubscribeEvent public void registerBlocks(RegistryEvent.Register<Block> event) { ModBlocks.registerBlocks(event); }
    @SubscribeEvent public void registerItems(RegistryEvent.Register<Item> event) { ModItems.registerItems(event); }
    @SubscribeEvent public void registerEntities(RegistryEvent.Register<EntityEntry> event) { ModEntities.registerEntities(event); }
    @SubscribeEvent public void registerEnchantments(RegistryEvent.Register<Enchantment> event) { ModEnchantments.registerEnchantments(event); }
    @SubscribeEvent public void registerPotions(RegistryEvent.Register<Potion> event) { ModPotions.registerPotions(event); }
    @SubscribeEvent public void registerPotionTypes(RegistryEvent.Register<PotionType> event) { ModPotions.registerPotionTypes(event); }
    @SubscribeEvent public void registerRecipes(RegistryEvent.Register<IRecipe> event) { ModRecipes.registerRecipes(event); }
    @SubscribeEvent(priority = EventPriority.LOW) public void registerRecipesLate(RegistryEvent.Register<IRecipe> event) { ModRecipes.registerLateRecipes(event); }
    @SubscribeEvent public void registerSounds(RegistryEvent.Register<SoundEvent> event) { ModSounds.registerSounds(event); }
    @SubscribeEvent public void registerBiomes(RegistryEvent.Register<Biome> event) { ModBiomes.registerBiomes(event); }

    // Load Events
    @SubscribeEvent public void loadLootTables(LootTableLoadEvent event) { EventHandler.loadLootTables(event); }

    // In-Game Events
    @SubscribeEvent public void isPotionApplicable(PotionEvent.PotionApplicableEvent event) { EventHandler.isPotionApplicable(event); }
    @SubscribeEvent public void applyBonemeal(BonemealEvent event) { EventHandler.applyBonemeal(event); }
    @SubscribeEvent public void playNoteBlock(NoteBlockEvent.Play event) { EventHandler.playNoteBlock(event); }
    @SubscribeEvent public void rightClickBlock(PlayerInteractEvent.RightClickBlock event) { EventHandler.rightClickBlock(event); }
}
