package edu.campusconnect.controller;

import edu.campusconnect.dto.TableRow;
import edu.campusconnect.entity.Department;
import edu.campusconnect.enums.Role;
import edu.campusconnect.form.DepartmentForm;
import edu.campusconnect.repository.DepartmentRepository;
import edu.campusconnect.service.AccessService;
import edu.campusconnect.service.EventService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class DepartmentController {
  private final DepartmentRepository departments;
  private final AccessService access;
  private final EventService events;

  @GetMapping("/admin/departments")
  public String list(@RequestParam(defaultValue = "0") int page, Model model) {
    return TableSupport.show(
        model,
        "Departments",
        List.of("Code", "Department name"),
        departments.findAll().stream()
            .map(
                d ->
                    TableRow.of(
                        "/admin/departments/" + d.getId() + "/edit", d.getCode(), d.getName()))
            .toList(),
        page,
        "",
        "/admin/departments/new",
        null);
  }

  @GetMapping({"/admin/departments/new", "/admin/departments/{id}/edit"})
  public String form(@PathVariable(required = false) Long id, Model model) {
    var form = new DepartmentForm();
    if (id != null) {
      var d = departments.findById(id).orElseThrow();
      form.setCode(d.getCode());
      form.setName(d.getName());
    }
    model.addAttribute("form", form);
    model.addAttribute("title", "Department");
    return "forms/department";
  }

  @PostMapping({"/admin/departments/new", "/admin/departments/{id}/edit"})
  @Transactional
  public String save(
      @PathVariable(required = false) Long id,
      @Valid @ModelAttribute("form") DepartmentForm form,
      BindingResult errors,
      Model model) {
    access.roles(Role.ADMIN);
    model.addAttribute("title", "Department");
    var existing = departments.findByCode(form.getCode());
    if (existing.isPresent() && !existing.get().getId().equals(id))
      errors.rejectValue("code", "duplicate", "Department code already exists");
    if (errors.hasErrors()) return "forms/department";
    var d = id == null ? new Department() : departments.findById(id).orElseThrow();
    d.setCode(form.getCode());
    d.setName(form.getName());
    departments.save(d);
    events.audit(access.current(), "Saved department", d.getId());
    return "redirect:/admin/departments";
  }
}
