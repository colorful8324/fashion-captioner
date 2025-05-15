package com.fashionai.captioning.fashion_captioner;

import com.fashionai.captioning.fashion_captioner.controller.AdminController;
import com.fashionai.captioning.fashion_captioner.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.ui.Model;
import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminControllerTest {

    private UserService userService;
    private AdminController adminController;

    @BeforeEach
    public void setup() {
        userService = Mockito.mock(UserService.class);
        adminController = new AdminController(userService);
    }

    @Test
    public void testCreateForm() {
        Model model = new ConcurrentModel();
        String viewName = adminController.createForm(model);
        assertThat(viewName).isEqualTo("admin/user-form");
        assertThat(model.containsAttribute("user")).isTrue();
    }
}


