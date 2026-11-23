package net.createballistics.registry;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.createballistics.CreateBallistics;
import net.createballistics.block.FireControlComputerBlock;
import net.createballistics.block.FireControlCoordinatorBlock;
import net.createballistics.block.FireControlNodeBlock;
import net.createballistics.util.Tooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 三个核心方块的注册。
 *
 * <ul>
 *   <li><b>火控节点</b> fire_control_node —— 挨着 CBC 炮座放，正面接应力</li>
 *   <li><b>火控协调器</b> fire_control_coordinator —— 组网与调度</li>
 *   <li><b>火控计算机</b> fire_control_computer —— 解算射击诸元</li>
 * </ul>
 */
public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateBallistics.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateBallistics.MODID);

    /** 与 CBC 的机匣同风格的金属方块属性。 */
    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(1.5F)
                .requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<FireControlNodeBlock> FIRE_CONTROL_NODE =
            BLOCKS.registerBlock("fire_control_node", FireControlNodeBlock::new, ModBlocks::metal);
    public static final DeferredItem<BlockItem> FIRE_CONTROL_NODE_ITEM =
            helpBlockItem("fire_control_node", FIRE_CONTROL_NODE, "block.create_ballistics.fire_control_node.help");

    public static final DeferredBlock<FireControlCoordinatorBlock> FIRE_CONTROL_COORDINATOR =
            BLOCKS.registerBlock("fire_control_coordinator", FireControlCoordinatorBlock::new, ModBlocks::metal);
    public static final DeferredItem<BlockItem> FIRE_CONTROL_COORDINATOR_ITEM =
            helpBlockItem("fire_control_coordinator", FIRE_CONTROL_COORDINATOR, "block.create_ballistics.fire_control_coordinator.help");

    public static final DeferredBlock<FireControlComputerBlock> FIRE_CONTROL_COMPUTER =
            BLOCKS.registerBlock("fire_control_computer", FireControlComputerBlock::new, ModBlocks::metal);
    public static final DeferredItem<BlockItem> FIRE_CONTROL_COMPUTER_ITEM =
            helpBlockItem("fire_control_computer", FIRE_CONTROL_COMPUTER, "block.create_ballistics.fire_control_computer.help");

    private static DeferredItem<BlockItem> helpBlockItem(String name, Supplier<? extends Block> block, String helpKey) {
        return ITEMS.registerItem(name, props -> new BlockItem(block.get(), props.useBlockDescriptionPrefix().stacksTo(64)) {
            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay,
                                        Consumer<Component> tooltip, TooltipFlag flag) {
                super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
                Tooltips.addLines(tooltip, helpKey, ChatFormatting.GRAY);
            }
        });
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
