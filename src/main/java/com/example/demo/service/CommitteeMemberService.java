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

    // ✅ 1. ส่วนที่ขาดไป (ต้นเหตุ Error)
    public List<CommitteeMember> getAllMembers() {
        return repository.findAll();
    }

    // ✅ 2. ส่วนสำหรับค้นหาตาม ID
    public CommitteeMember getMemberById(Long id) {
        return repository.findById(id).orElse(null);
    }

    // ✅ 3. ส่วนสำหรับบันทึกข้อมูล
    public CommitteeMember createMember(CommitteeMember member) {
        return repository.save(member);
    }

    // ✅ 4. อัปเดตข้อมูลตาม ID
    public CommitteeMember updateMember(Long id, CommitteeMember updated) {
        return repository.findById(id).map(existing -> {

            existing.setCitizenId(updated.getCitizenId());
            existing.setPrename(updated.getPrename());
            existing.setFirstname(updated.getFirstname());
            existing.setLastname(updated.getLastname());
            existing.setAffiliation(updated.getAffiliation());
            existing.setDepartment(updated.getDepartment());
            existing.setPhone(updated.getPhone());
            existing.setEmail(updated.getEmail());

            // updated_at จะอัปเดตเอง เพราะใช้:
            // DEFAULT_GENERATED on update CURRENT_TIMESTAMP

            return repository.save(existing);
        }).orElse(null);
    }
}