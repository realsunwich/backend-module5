package com.example.demo.controller;

import com.example.demo.entity.CommitteeMember;
import com.example.demo.service.CommitteeMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List; // 👈 อย่าลืม import List ด้วยนะครับ

@RestController
@RequestMapping("/api/committee-members")
@CrossOrigin(origins = "*") // แนะนำให้ใช้ * ช่วง Dev เพื่อลดปัญหา CORS
public class CommitteeMemberController {

    @Autowired
    private CommitteeMemberService service;

    // ✅ 1. ส่วนที่เพิ่มเข้ามา: ดึงข้อมูลทั้งหมด (GET /api/committee-members)
    @GetMapping
    public List<CommitteeMember> getAllMembers() {
        return service.getAllMembers();
    }

    // 2. ดึงข้อมูลตาม ID (GET /api/committee-members/{id})
    @GetMapping("/{id}")
    public CommitteeMember getMemberById(@PathVariable Long id) {
        return service.getMemberById(id);
    }

    // 3. เพิ่มรายชื่อใหม่ (POST /api/committee-members)
    @PostMapping
    public CommitteeMember addMember(@RequestBody CommitteeMember member) {
        return service.createMember(member);
    }
}