package com.fashionai.captioning.fashion_captioner.controller;

import com.fashionai.captioning.fashion_captioner.model.h2.User;
import com.fashionai.captioning.fashion_captioner.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;

    @GetMapping("dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/user")
    public String list(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/user";
    }

    @GetMapping("/user/new")
    public String createForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("isEdit", false);
        return "admin/user-form :: userFormModal";
    }

    @PostMapping("/user/save")
    public String save(@ModelAttribute("user") User user) {
        System.out.println("[INFO] Đã nhận yêu cầu POST /admin/user/save");
        System.out.println("[INFO] Dữ liệu user từ form: " + user);

        try {
            userService.saveUser(user);
            System.out.println("[INFO] User đã được lưu thành công. ID: " + user.getId());
        } catch (Exception e) {
            System.out.println("[ERROR] Không thể lưu user. Dữ liệu: " + user);
            e.printStackTrace(); // In stack trace lỗi ra terminal
            return "redirect:/admin/user?error=savefail";
        }

        return "redirect:/admin/user";
    }


    @GetMapping("/user/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            model.addAttribute("user", user.get());
            model.addAttribute("isEdit", true);
            return "admin/user-form :: userFormModal";
        }
        return "redirect:/admin/user";
    }

    @PostMapping("/user/{id}")
    public String update(@PathVariable Long id, @ModelAttribute User userFromForm) {
        Optional<User> optionalUser = userService.getUserById(id);
        if (optionalUser.isEmpty()) {
            // Xử lý user không tồn tại (có thể redirect hoặc báo lỗi)
            return "redirect:/admin/user?error=notfound";
        }
        User userInDb = optionalUser.get();

        // Cập nhật từng trường mà form gửi lên (ví dụ)
        userInDb.setName(userFromForm.getName());
        userInDb.setEmail(userFromForm.getEmail());
        userInDb.setUserStatus(userFromForm.getUserStatus());
        userInDb.setRole(userFromForm.getRole());
        // ... những trường khác nếu có trong form

        userService.saveUser(userInDb);

        return "redirect:/admin/user";
    }




    @GetMapping("/user/delete/{id}")
    public String delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/user";
    }
}
