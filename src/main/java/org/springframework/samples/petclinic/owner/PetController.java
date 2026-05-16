/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Spring MVC {@link org.springframework.stereotype.Controller} that handles all
 * pet-related HTTP endpoints under {@code /owners/{ownerId}/pets}.
 *
 * <p>
 * This controller coordinates the pet creation and editing flows on behalf of a specific
 * owner. It interacts with both {@link OwnerRepository} (to load and persist the owner
 * aggregate, which cascades to pets) and {@link PetTypeRepository} (to populate the pet
 * type dropdown). Responsibilities include:
 * </p>
 * <ul>
 * <li>Rendering and processing the pet creation and edit forms.</li>
 * <li>Enforcing duplicate-name validation within an owner's existing pets.</li>
 * <li>Rejecting birth dates set in the future.</li>
 * </ul>
 *
 * <p>
 * {@link PetValidator} is registered via {@link #initPetBinder} and runs automatically
 * before every pet form submission. Both owner and pet binders block client-submitted
 * {@code id} values to prevent mass-assignment.
 * </p>
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Wick Dynex
 * @see OwnerRepository
 * @see PetTypeRepository
 * @see PetValidator
 */
@Controller
@RequestMapping("/owners/{ownerId}")
class PetController {

	/** View name for the pet creation and update form. */
	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private static final String REDIRECT_TO_OWNER = "redirect:/owners/{ownerId}";

	private static final String DUPLICATE_PET_MESSAGE = "already exists";

	private final OwnerRepository owners;

	private final PetTypeRepository types;

	/**
	 * Creates a new {@code PetController} backed by the given repositories.
	 * @param owners the repository used to load and persist owners (and their pets via
	 * cascade)
	 * @param types the repository used to populate the pet-type dropdown
	 */
	public PetController(OwnerRepository owners, PetTypeRepository types) {
		this.owners = owners;
		this.types = types;
	}

	/**
	 * Populates the {@code types} model attribute with all available pet types for use in
	 * the form dropdown.
	 * @return collection of all {@link PetType} records
	 */
	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return this.types.findPetTypes();
	}

	/**
	 * Loads the {@link Owner} for the current request before each handler method runs.
	 * @param ownerId the owner's database id from the URL path variable
	 * @return the matching {@link Owner}
	 * @throws IllegalArgumentException if no owner with {@code ownerId} exists
	 */
	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable("ownerId") int ownerId) {
		return findRequiredOwner(ownerId);
	}

	/**
	 * Resolves the {@link Pet} model attribute before each handler method runs.
	 *
	 * <p>
	 * Returns a new, empty {@link Pet} when no {@code petId} path variable is present
	 * (the creation flow). When {@code petId} is provided (the edit flow), the pet is
	 * looked up on the owner.
	 * </p>
	 * @param ownerId the owner's database id from the URL path variable
	 * @param petId the pet's database id, or {@code null} on the creation flow
	 * @return an existing {@link Pet} or a fresh instance
	 * @throws IllegalArgumentException if the owner is not found
	 */
	@ModelAttribute("pet")
	public Pet findPet(@PathVariable("ownerId") int ownerId,
			@PathVariable(name = "petId", required = false) Integer petId) {

		if (petId == null) {
			return new Pet();
		}

		Owner owner = findRequiredOwner(ownerId);
		return owner.getPet(petId);
	}

	private Owner findRequiredOwner(int ownerId) {
		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		return optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct "));
	}

	/**
	 * Prevents client-supplied {@code id} values from being bound to the owner model
	 * object.
	 * @param dataBinder the data binder for the {@code owner} model attribute
	 */
	@InitBinder("owner")
	public void initOwnerBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Registers {@link PetValidator} as the validator for {@link Pet} form submissions
	 * and prevents client-supplied {@code id} values from being bound.
	 * @param dataBinder the data binder for the {@code pet} model attribute
	 */
	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetValidator());
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Renders the blank pet creation form, adding a transient {@link Pet} to the owner
	 * for form binding.
	 * @param owner the owner to whom the new pet will belong
	 * @return view name for the create/update pet form
	 */
	@GetMapping("/pets/new")
	public String initCreationForm(Owner owner) {
		Pet pet = new Pet();
		owner.addPet(pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Validates and persists a new pet for the given owner.
	 *
	 * <p>
	 * Rejects the submission if the pet's name duplicates an existing saved pet on this
	 * owner, or if the birth date is in the future. On success, saves the owner (which
	 * cascades to the new pet) and redirects to the owner's detail page.
	 * </p>
	 * @param owner the owner to whom the new pet will be added
	 * @param pet the form-bound pet to create; validated by {@link PetValidator}
	 * @param result binding and validation errors
	 * @param redirectAttributes flash attributes for the redirect response
	 * @return redirect to the owner's detail page, or the form view on error
	 */
	@PostMapping("/pets/new")
	public String processCreationForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes) {

		if (StringUtils.hasText(pet.getName()) && pet.isNew() && owner.getPet(pet.getName(), true) != null) {
			result.rejectValue("name", "duplicate", DUPLICATE_PET_MESSAGE);
		}

		rejectFutureBirthDate(pet, result);

		if (result.hasErrors()) {
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		owner.addPet(pet);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "New Pet has been Added");
		return REDIRECT_TO_OWNER;
	}

	/**
	 * Renders the pet edit form for an existing pet.
	 * @return view name for the create/update pet form
	 */
	@GetMapping("/pets/{petId}/edit")
	public String initUpdateForm() {
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Validates and persists updates to an existing pet.
	 *
	 * <p>
	 * Rejects the submission if the new name duplicates another pet already belonging to
	 * this owner (excluding the pet being edited), or if the birth date is in the future.
	 * Delegates the actual field update and save to {@link #updatePetDetails}.
	 * </p>
	 * @param owner the owner of the pet being edited
	 * @param pet the form-bound pet with updated values; validated by
	 * {@link PetValidator}
	 * @param result binding and validation errors
	 * @param redirectAttributes flash attributes for the redirect response
	 * @return redirect to the owner's detail page, or the form view on error
	 */
	@PostMapping("/pets/{petId}/edit")
	public String processUpdateForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes) {

		String petName = pet.getName();

		// checking if the pet name already exists for the owner
		Pet existingPet = owner.getPet(petName, false);
		if (StringUtils.hasText(petName) && existingPet != null && !Objects.equals(existingPet.getId(), pet.getId())) {
			result.rejectValue("name", "duplicate", DUPLICATE_PET_MESSAGE);
		}

		rejectFutureBirthDate(pet, result);

		if (result.hasErrors()) {
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		updatePetDetails(owner, pet);
		redirectAttributes.addFlashAttribute("message", "Pet details has been edited");
		return REDIRECT_TO_OWNER;
	}

	/**
	 * Updates the fields of the existing pet in-place, or adds the pet to the owner if no
	 * matching id is found, then saves the owner aggregate.
	 * @param owner the owner of the pet
	 * @param pet the pet carrying the updated field values
	 * @throws IllegalStateException if {@code pet.getId()} is {@code null}
	 */
	private void updatePetDetails(Owner owner, Pet pet) {
		Integer id = pet.getId();
		Assert.state(id != null, "'pet.getId()' must not be null");
		Pet existingPet = owner.getPet(id);
		if (existingPet != null) {
			// Update existing pet's properties
			existingPet.setName(pet.getName());
			existingPet.setBirthDate(pet.getBirthDate());
			existingPet.setType(pet.getType());
		}
		else {
			owner.addPet(pet);
		}
		this.owners.save(owner);
	}

	private void rejectFutureBirthDate(Pet pet, BindingResult result) {
		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}
	}

}
