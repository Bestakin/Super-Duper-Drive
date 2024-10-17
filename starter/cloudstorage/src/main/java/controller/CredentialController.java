package controller;

import com.udacity.jwdnd.course1.cloudstorage.services.CredentialService;
import com.udacity.jwdnd.course1.cloudstorage.services.EncryptionService;
import com.udacity.jwdnd.course1.cloudstorage.services.UserService;
import model.Credential;
import model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.File;
import java.security.Principal;

@Controller
public class CredentialController {
    private final CredentialService credentialService;
    private final EncryptionService encryptionService;
    private final UserService userService;

    public CredentialController(CredentialService credentialService, EncryptionService encryptionService, UserService userService) {
        this.credentialService = credentialService;
        this.encryptionService = encryptionService;
        this.userService = userService;
    }

    // Create a new credential
    @PostMapping("/credential")
    public String store(Model model, @ModelAttribute Credential form, Principal principal, RedirectAttributes redirectAttrs) {
        String error = null;
        User user = this.userService.getUser(principal.getName());
        String key = this.encryptionService.getEncryptionKey();  // Access EncryptionService directly

        if (error == null) {
            Credential credential = new Credential(null, form.getUrl(), form.getUsername(), key,
                    encryptionService.encryptValue(form.getPassword(), key), user.getUserId());
            int rowsAdded = this.credentialService.createCredential(credential);
            if (rowsAdded < 0) {
                error = "There was an error creating your credential. Please try again.";
            }
        }

        if (error == null) {
            redirectAttrs.addFlashAttribute("success", "Credential was successfully created");
        } else {
            redirectAttrs.addFlashAttribute("error", error);
        }

        return "redirect:/home";  // Redirect to home page after successful creation
    }

    // Delete a credential
    @GetMapping("/credential/delete/{id}")
    public String deleteCredential(@PathVariable String id, RedirectAttributes redirectAttrs) {
        String error = null;
        int rowsDeleted = this.credentialService.deleteRecordId(Integer.parseInt(id));

        if (rowsDeleted < 0) {
            error = "There was an error deleting your credential at the moment. Please try again.";
        }

        if (error == null) {
            redirectAttrs.addFlashAttribute("success", "Credential was successfully deleted.");
        } else {
            redirectAttrs.addFlashAttribute("error", error);
        }

        return "redirect:/home";  // Redirect to home page after deletion
    }

    // Show the form to update a credential
    @GetMapping("/credential/edit/{id}")
    public String showUpdateForm(@PathVariable("id") String id, Model model) {
        Credential credential = this.credentialService.getCredentialById(Integer.valueOf(id));

        // Decrypt the password before showing the form
        credential.setPassword(this.encryptionService.decryptValue(credential.getPassword(), credential.getKey()));
        model.addAttribute("credentialEdit", credential);

        return "update/update-credential";  // Thymeleaf template for updating credentials
    }

    // Update a credential
    @PostMapping("/credential/update/{id}")
    public String updateCredential(@PathVariable("id") String id, @Valid Credential credential,
                                   BindingResult result, RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            credential.setCredentialId(Integer.valueOf(id));
            return "update/update-credential";  // Stay on the same page if there are validation errors
        }

        // Update the encryption key and the password before updating the record
        credential.setKey(encryptionService.getEncryptionKey());
        credential.setPassword(encryptionService.encryptValue(credential.getPassword(), credential.getKey()));
        int updateRow = this.credentialService.updateCredential(credential, Integer.valueOf(id));

        if (updateRow > 0) {
            redirectAttrs.addFlashAttribute("success", "Credential was successfully updated");
        } else {
            redirectAttrs.addFlashAttribute("error", "We could not update your credential at the moment, please try again later.");
        }

        return "redirect:/home";  // Redirect to home page after update
    }
}
