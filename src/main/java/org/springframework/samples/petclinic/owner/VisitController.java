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

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Spring MVC {@link org.springframework.stereotype.Controller} that handles scheduling
 * vet visits under {@code /owners/{ownerId}/pets/{petId}/visits}.
 *
 * <p>
 * This controller coordinates visit creation on behalf of a specific owner and pet. It
 * interacts with {@link OwnerRepository} to load the owner/pet context and to persist
 * visits through the owner aggregate (the {@code CascadeType.ALL} on
 * {@link Owner#getPets()} means saving the owner automatically saves new visits). The
 * controller has a single entry point for booking new visits; reviewing past visits is
 * handled by the owner detail view.
 * </p>
 *
 * <p>
 * The {@link #loadPetWithVisit} {@code @ModelAttribute} method runs before both the GET
 * and POST handlers, ensuring the model always contains fresh {@code owner}, {@code pet},
 * and {@code visit} objects. The {@link #setAllowedFields} binder blocks client-submitted
 * {@code id} values from being bound to any model object.
 * </p>
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 * @author Wick Dynex
 * @see OwnerRepository
 * @see Owner#addVisit(Integer, Visit)
 */
@Controller
class VisitController {

	private final OwnerRepository owners;

	/**
	 * Creates a new {@code VisitController} backed by the given repository.
	 * @param owners the repository used to load owners and persist visits via cascade
	 */
	public VisitController(OwnerRepository owners) {
		this.owners = owners;
	}

	/**
	 * Prevents client-supplied {@code id} values from being bound to any model object,
	 * guarding against mass-assignment attacks.
	 * @param dataBinder the data binder to configure
	 */
	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Loads the owner and pet from the database, populates the model, and creates a
	 * transient {@link Visit} attached to the pet.
	 *
	 * <p>
	 * This method runs automatically before both the GET and POST handlers via the
	 * {@code @ModelAttribute} mechanism, ensuring the model always contains up-to-date
	 * {@code owner} and {@code pet} objects. A new {@link Visit} instance is created and
	 * attached to the pet so the form has a bound object to render.
	 * </p>
	 * @param ownerId the owner's database id from the URL path variable
	 * @param petId the pet's database id from the URL path variable
	 * @param model the raw model map used to expose {@code owner} and {@code pet}
	 * @return a new, transient {@link Visit} attached to the resolved pet
	 * @throws IllegalArgumentException if the owner or pet is not found
	 */
	@ModelAttribute("visit")
	public Visit loadPetWithVisit(@PathVariable("ownerId") int ownerId, @PathVariable("petId") int petId,
			Map<String, Object> model) {
		Optional<Owner> optionalOwner = owners.findById(ownerId);
		Owner owner = optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct "));

		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new IllegalArgumentException(
					"Pet with id " + petId + " not found for owner with id " + ownerId + ".");
		}
		model.put("pet", pet);
		model.put("owner", owner);

		Visit visit = new Visit();
		pet.addVisit(visit);
		return visit;
	}

	/**
	 * Renders the new visit booking form. {@link #loadPetWithVisit} has already populated
	 * the model with the owner, pet, and a fresh visit.
	 * @return view name for the create/update visit form
	 */
	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String initNewVisitForm() {
		return "pets/createOrUpdateVisitForm";
	}

	/**
	 * Validates and persists a new vet visit.
	 *
	 * <p>
	 * Delegates to {@link Owner#addVisit(Integer, Visit)} to attach the visit to the
	 * correct pet, then saves the owner aggregate (which cascades to the new visit).
	 * Returns the form view on validation errors; redirects to the owner's detail page on
	 * success.
	 * </p>
	 * @param owner the owner resolved by {@link #loadPetWithVisit}
	 * @param petId the pet's database id from the URL path variable
	 * @param visit the form-bound visit to persist; validated by Bean Validation
	 * @param result binding and validation errors
	 * @param redirectAttributes flash attributes for the redirect response
	 * @return redirect to the owner's detail page, or the form view on error
	 */
	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String processNewVisitForm(@ModelAttribute Owner owner, @PathVariable int petId, @Valid Visit visit,
			BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		}

		owner.addVisit(petId, visit);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "Your visit has been booked");
		return "redirect:/owners/{ownerId}";
	}

}
