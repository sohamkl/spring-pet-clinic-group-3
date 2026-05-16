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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Spring MVC {@link org.springframework.stereotype.Controller} that handles all
 * owner-related HTTP endpoints under {@code /owners/**}.
 *
 * <p>
 * This controller forms the primary entry point for the owner-management slice of the
 * application. It coordinates with {@link OwnerRepository} to create, search, update, and
 * display owners. Responsibilities include:
 * </p>
 * <ul>
 * <li>Rendering and processing the owner creation and edit forms.</li>
 * <li>Searching owners by last name with paginated results.</li>
 * <li>Displaying full owner details including their pets and visits.</li>
 * </ul>
 *
 * <p>
 * The {@link #setAllowedFields} binder prevents client-submitted {@code id} values from
 * being bound to any model object, guarding against mass-assignment vulnerabilities. The
 * {@link #findOwner} {@code @ModelAttribute} method pre-loads the correct {@link Owner}
 * instance before every handler that declares an {@code ownerId} path variable.
 * </p>
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Wick Dynex
 * @see OwnerRepository
 * @see Owner
 */
@Controller
class OwnerController {

	/** View name for the owner creation and update form. */
	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	private static final String REDIRECT_TO_OWNER_PREFIX = "redirect:/owners/";

	private static final String REDIRECT_TO_OWNER = "redirect:/owners/{ownerId}";

	private static final String REDIRECT_TO_OWNER_EDIT = "redirect:/owners/{ownerId}/edit";

	private static final String OWNER_NOT_FOUND_MESSAGE_PREFIX = "Owner not found with id: ";

	private static final String OWNER_NOT_FOUND_MESSAGE_SUFFIX = ". Please ensure the ID is correct.";

	private final OwnerRepository owners;

	/**
	 * Creates a new {@code OwnerController} backed by the given repository.
	 * @param owners the repository used to load and persist owners
	 */
	public OwnerController(OwnerRepository owners) {
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
	 * Resolves the {@link Owner} model attribute before each handler method runs.
	 *
	 * <p>
	 * Returns a new, empty {@link Owner} when no {@code ownerId} path variable is present
	 * (the creation flow). When {@code ownerId} is provided (the edit or detail flow),
	 * the owner is loaded from the database; an {@link IllegalArgumentException} is
	 * thrown if no matching record exists.
	 * </p>
	 * @param ownerId the owner's database id, or {@code null} on the creation flow
	 * @return an existing {@link Owner} or a fresh instance
	 * @throws IllegalArgumentException if {@code ownerId} is non-null but not found
	 */
	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		return ownerId == null ? new Owner()
				: this.owners.findById(ownerId)
					.orElseThrow(() -> new IllegalArgumentException(
							OWNER_NOT_FOUND_MESSAGE_PREFIX + ownerId + OWNER_NOT_FOUND_MESSAGE_SUFFIX));
	}

	/**
	 * Renders the blank owner creation form.
	 * @return view name for the create/update form
	 */
	@GetMapping("/owners/new")
	public String initCreationForm() {
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Validates and persists a new owner.
	 *
	 * <p>
	 * Returns the form view on validation errors so the user can correct them. On
	 * success, persists the owner and redirects to their detail page.
	 * </p>
	 * @param owner the form-bound owner to create; validated by Bean Validation
	 * @param result binding and validation errors
	 * @param redirectAttributes flash attributes for the redirect response
	 * @return redirect to the new owner's detail page, or the form view on error
	 */
	@PostMapping("/owners/new")
	public String processCreationForm(@Valid Owner owner, BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in creating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "New Owner Created");
		return REDIRECT_TO_OWNER_PREFIX + owner.getId();
	}

	/**
	 * Renders the owner search form.
	 * @return view name for the find-owners page
	 */
	@GetMapping("/owners/find")
	public String initFindForm() {
		return "owners/findOwners";
	}

	/**
	 * Searches owners by last name and renders paginated results.
	 *
	 * <p>
	 * A missing or empty {@code lastName} is treated as a wildcard, returning all owners.
	 * If exactly one match is found, the user is redirected directly to that owner's
	 * detail page. If no matches are found, a field error is added and the search form is
	 * re-rendered.
	 * </p>
	 * @param page the 1-based page number; defaults to {@code 1}
	 * @param owner binds the {@code lastName} search parameter from the request
	 * @param result binding errors from the owner form binding
	 * @param model the Spring MVC model for passing data to the view
	 * @return redirect to a single owner's detail page, or the owners list / search form
	 */
	@GetMapping("/owners")
	public String processFindForm(@RequestParam(defaultValue = "1") int page, Owner owner, BindingResult result,
			Model model) {
		// allow parameterless GET request for /owners to return all records
		String lastName = owner.getLastName();
		if (lastName == null) {
			lastName = ""; // empty string signifies broadest possible search
		}

		// find owners by last name
		Page<Owner> ownersResults = findPaginatedForOwnersLastName(page, lastName);
		if (ownersResults.isEmpty()) {
			// no owners found
			result.rejectValue("lastName", "notFound", "not found");
			return "owners/findOwners";
		}

		if (ownersResults.getTotalElements() == 1) {
			// 1 owner found
			owner = ownersResults.iterator().next();
			return REDIRECT_TO_OWNER_PREFIX + owner.getId();
		}

		// multiple owners found
		return addPaginationModel(page, model, ownersResults);
	}

	/**
	 * Populates the model with pagination metadata and the current page of owners.
	 * @param page the current 1-based page number
	 * @param model the Spring MVC model
	 * @param paginated the paginated query result
	 * @return view name for the owners list page
	 */
	private String addPaginationModel(int page, Model model, Page<Owner> paginated) {
		List<Owner> listOwners = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listOwners", listOwners);
		return "owners/ownersList";
	}

	/**
	 * Queries the repository for owners whose last name starts with {@code lastname},
	 * returning one fixed-size page of results.
	 * @param page the 1-based page number
	 * @param lastname the last-name prefix to search for
	 * @return one page of matching {@link Owner} records
	 */
	private Page<Owner> findPaginatedForOwnersLastName(int page, String lastname) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return owners.findByLastNameStartingWith(lastname, pageable);
	}

	/**
	 * Renders the owner edit form for an existing owner.
	 * @return view name for the create/update form
	 */
	@GetMapping("/owners/{ownerId}/edit")
	public String initUpdateOwnerForm() {
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Validates and persists updates to an existing owner.
	 *
	 * <p>
	 * Guards against a mismatch between the {@code ownerId} path variable and the id
	 * embedded in the submitted form before saving. Returns the form view on any error.
	 * </p>
	 * @param owner the form-bound owner with updated values; validated by Bean Validation
	 * @param result binding and validation errors
	 * @param ownerId the owner id from the URL path variable
	 * @param redirectAttributes flash attributes for the redirect response
	 * @return redirect to the owner's detail page, or the form view on error
	 */
	@PostMapping("/owners/{ownerId}/edit")
	public String processUpdateOwnerForm(@Valid Owner owner, BindingResult result, @PathVariable("ownerId") int ownerId,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in updating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		if (!Objects.equals(owner.getId(), ownerId)) {
			result.rejectValue("id", "mismatch", "The owner ID in the form does not match the URL.");
			redirectAttributes.addFlashAttribute("error", "Owner ID mismatch. Please try again.");
			return REDIRECT_TO_OWNER_EDIT;
		}

		owner.setId(ownerId);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "Owner Values Updated");
		return REDIRECT_TO_OWNER;
	}

	/**
	 * Loads and displays the detail page for a single owner, including their pets and
	 * visits.
	 * @param ownerId the ID of the owner to display
	 * @return a {@link ModelAndView} containing the owner object and the detail view name
	 * @throws IllegalArgumentException if no owner with {@code ownerId} exists
	 */
	@GetMapping("/owners/{ownerId}")
	public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
		ModelAndView mav = new ModelAndView("owners/ownerDetails");
		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		Owner owner = optionalOwner.orElseThrow(() -> new IllegalArgumentException(
				OWNER_NOT_FOUND_MESSAGE_PREFIX + ownerId + OWNER_NOT_FOUND_MESSAGE_SUFFIX));
		mav.addObject(owner);
		return mav;
	}

}
