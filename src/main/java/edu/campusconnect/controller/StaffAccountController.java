package edu.campusconnect.controller;

import edu.campusconnect.dto.TableRow;
import edu.campusconnect.enums.Role;
import edu.campusconnect.form.StaffAccountForm;
import edu.campusconnect.repository.UserRepository;
import edu.campusconnect.service.StaffAccountService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class StaffAccountController {
  private final UserRepository users;
  private final StaffAccountService service;

  @GetMapping("/admin/coordinators")
  public String list(@RequestParam(defaultValue = "0") int page, Model m) {
    return TableSupport.show(
        m,
        "Placement coordinators",
        List.of("Name", "Email", "Account"),
        users.findByRole(Role.COORDINATOR).stream()
            .map(
                u ->
                    TableRow.of(
                        null, u.getFullName(), u.getEmail(), u.isEnabled() ? "Active" : "Disabled"))
            .toList(),
        page,
        "",
        "/admin/coordinators/new",
        null);
  }

  @GetMapping("/admin/coordinators/new")
  public String form(Model m) {
    m.addAttribute("form", new StaffAccountForm());
    m.addAttribute("title", "Create placement coordinator");
    return "forms/coordinator";
  }

  @PostMapping("/admin/coordinators/new")
  public String save(
      @Valid @ModelAttribute("form") StaffAccountForm f, BindingResult errors, Model m) {
    m.addAttribute("title", "Create placement coordinator");
    if (errors.hasErrors()) return "forms/coordinator";
    service.create(f);
    return "redirect:/admin/coordinators";
  }
}
