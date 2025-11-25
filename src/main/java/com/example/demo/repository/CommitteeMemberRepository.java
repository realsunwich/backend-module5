package com.example.demo.repository;

import com.example.demo.entity.CommitteeMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitteeMemberRepository extends JpaRepository<CommitteeMember, Long> {
    // ถ้าต้องการค้นหาเพิ่ม เช่น หาจากชื่อ
    // List<CommitteeMember> findByFirstnameContaining(String name);
}