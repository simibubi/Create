package com.simibubi.create.content.contraptions.itemAssembly;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.contraptions.processing.ProcessingOutput;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipe;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder.ProcessingRecipeFactory;

import net.minecraft.data.IFinishedRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;

public class SequencedAssemblyRecipeBuilder {

	private SequencedAssemblyRecipe recipe;
	protected List<ICondition> recipeConditions;

	public SequencedAssemblyRecipeBuilder(ResourceLocation id) {
		recipeConditions = new ArrayList<>();
		this.recipe = new SequencedAssemblyRecipe(id,
			AllRecipeTypes.SEQUENCED_ASSEMBLY.getSerializer());
	}

	public <T extends ProcessingRecipe<?>> SequencedAssemblyRecipeBuilder addStep(ProcessingRecipeFactory<T> factory,
		UnaryOperator<ProcessingRecipeBuilder<T>> builder) {
		ProcessingRecipeBuilder<T> recipeBuilder =
			new ProcessingRecipeBuilder<>(factory, new ResourceLocation("dummy"));
		Item placeHolder = recipe.getTransitionalItem()
			.getItem();
		recipe.getSequence()
			.add(new SequencedRecipe<>(builder.apply(recipeBuilder.require(placeHolder)
				.output(placeHolder))
				.build()));
		return this;
	}

	public SequencedAssemblyRecipeBuilder require(IItemProvider ingredient) {
		return require(Ingredient.of(ingredient));
	}

	public SequencedAssemblyRecipeBuilder require(ITag.INamedTag<Item> tag) {
		return require(Ingredient.of(tag));
	}

	public SequencedAssemblyRecipeBuilder require(Ingredient ingredient) {
		recipe.ingredient = ingredient;
		return this;
	}

	public SequencedAssemblyRecipeBuilder transitionTo(IItemProvider item) {
		recipe.transitionalItem = new ProcessingOutput(new ItemStack(item), 1);
		return this;
	}

	public SequencedAssemblyRecipeBuilder loops(int loops) {
		recipe.loops = loops;
		return this;
	}

	public SequencedAssemblyRecipeBuilder addOutput(IItemProvider item, float weight) {
		return addOutput(new ItemStack(item), weight);
	}

	public SequencedAssemblyRecipeBuilder addOutput(ItemStack item, float weight) {
		recipe.resultPool.add(new ProcessingOutput(item, weight));
		return this;
	}

	public SequencedAssemblyRecipe build() {
		return recipe;
	}

	public void build(Consumer<IFinishedRecipe> consumer) {
		consumer.accept(new DataGenResult(build(), recipeConditions));
	}

	public static class DataGenResult implements IFinishedRecipe {

		private SequencedAssemblyRecipe recipe;
		private List<ICondition> recipeConditions;
		private ResourceLocation id;
		private SequencedAssemblyRecipeSerializer serializer;

		public DataGenResult(SequencedAssemblyRecipe recipe, List<ICondition> recipeConditions) {
			this.recipeConditions = recipeConditions;
			this.recipe = recipe;
			this.id = new ResourceLocation(recipe.getId().getNamespace(),
					AllRecipeTypes.SEQUENCED_ASSEMBLY.getId().getPath() + "/" + recipe.getId().getPath());
			this.serializer = (SequencedAssemblyRecipeSerializer) recipe.getSerializer();
		}

		@Override
		public void serializeRecipeData(JsonObject json) {
			serializer.write(json, recipe);
			if (recipeConditions.isEmpty())
				return;

			JsonArray conds = new JsonArray();
			recipeConditions.forEach(c -> conds.add(CraftingHelper.serialize(c)));
			json.add("conditions", conds);
		}

		@Override
		public ResourceLocation getId() {
			return id;
		}

		@Override
		public IRecipeSerializer<?> getType() {
			return serializer;
		}

		@Override
		public JsonObject serializeAdvancement() {
			return null;
		}

		@Override
		public ResourceLocation getAdvancementId() {
			return null;
		}

	}

}
