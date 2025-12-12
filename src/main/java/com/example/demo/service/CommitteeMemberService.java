package com.example.demo.service;

import com.example.demo.entity.CommitteeMember;
import com.example.demo.repository.CommitteeMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CommitteeMemberService {

    @Autowired
    private CommitteeMemberRepository repository;

    // 1. ดึงข้อมูลทั้งหมด
    public List<CommitteeMember> getAllMembers() {
        return repository.findAll();
    }

    // 2. ค้นหาตาม ID
    public CommitteeMember getMemberById(Long id) {
        return repository.findById(id).orElse(null);
    }

    // 3. บันทึกข้อมูลใหม่
    public CommitteeMember createMember(CommitteeMember member) {
        return repository.save(member);
    }

    // 4. อัปเดตข้อมูลตาม ID (ปรับปรุงให้ครบทุกฟิลด์)
    public CommitteeMember updateMember(Long id, CommitteeMember updated) {
        return repository.findById(id).map(existing -> {

            // --- ข้อมูลบัตรประชาชน ---
            existing.setCitizenId(updated.getCitizenId());
            existing.setLaserId(updated.getLaserId()); // เพิ่ม Laser ID

            // --- ชื่อภาษาไทย ---
            existing.setPrename(updated.getPrename());
            existing.setFirstname(updated.getFirstname());
            existing.setMiddlename(updated.getMiddlename()); // เพิ่มชื่อกลางไทย
            existing.setLastname(updated.getLastname());

            // --- ชื่อภาษาอังกฤษ (เพิ่มใหม่) ---
            existing.setPrenameEn(updated.getPrenameEn());
            existing.setFirstnameEn(updated.getFirstnameEn());
            existing.setMiddlenameEn(updated.getMiddlenameEn());
            existing.setLastnameEn(updated.getLastnameEn());

            // --- ข้อมูลส่วนตัว ---
            existing.setBirthdate(updated.getBirthdate()); // เพิ่มวันเกิด

            // --- ข้อมูลการทำงาน/ติดต่อ ---
            existing.setAffiliation(updated.getAffiliation());
            existing.setDepartment(updated.getDepartment());
            existing.setPhone(updated.getPhone());
            existing.setEmail(updated.getEmail());

            // หมายเหตุ: updated_at ถ้าจัดการในระดับ Database (Trigger/Default)
            // ก็ไม่ต้องทำอะไรใน JPA แต่ถ้าอยากให้ JPA จัดการ ให้เพิ่ม field
            // @LastModifiedDate ใน Entity

            return repository.save(existing);
        }).orElse(null);
    }

    // 5. (Optional) ส่วนสำหรับลบข้อมูล
    public void deleteMember(Long id) {
        repository.deleteById(id);
    }
}