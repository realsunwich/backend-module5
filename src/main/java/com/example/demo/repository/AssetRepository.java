package com.example.demo.repository;

import com.example.demo.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByStatus(Asset.AssetStatus status);

    long countByStatus(Asset.AssetStatus status);

    List<Asset> findByAssetType(String assetType);

    // Query Methods สำหรับ Intent-Based System

    // ดึงทรัพย์สินล่าสุด (จำกัดจำนวน)
    List<Asset> findTop20ByOrderByIdDesc();

    // ดึงทรัพย์สินตามประเภทและสถานะ (จำกัดจำนวน)
    List<Asset> findTop20ByAssetTypeAndStatus(String assetType, Asset.AssetStatus status);

    // นับทรัพย์สินตามประเภท
    long countByAssetType(String assetType);
}