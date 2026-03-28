package com.project.controller;

import com.project.service.CommentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/admin/comments")
    public String list(Model model) {
        model.addAttribute("pendingComments", commentService.listPending());
        model.addAttribute("approvedComments", commentService.listApproved());
        return "admin/comments/list";
    }

    @PostMapping("/admin/comments/{id}/approve")
    public String approve(@PathVariable Long id) {
        commentService.approve(id);
        return "redirect:/admin/comments";
    }

    @PostMapping("/admin/comments/{id}/delete")
    public String delete(@PathVariable Long id) {
        commentService.delete(id);
        return "redirect:/admin/comments";
    }
}
