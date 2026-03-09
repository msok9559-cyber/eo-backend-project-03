package com.example.prompt.service;

import com.example.prompt.dto.admin.AdminDto;
import com.example.prompt.dto.admin.AdminUserDetailDto;
import com.example.prompt.dto.admin.AdminUserDto;
import com.example.prompt.dto.admin.DashboardDto;

import java.util.List;

public interface AdminService {

    /**
     * 관리자 로그인
     */
    AdminDto.LoginResponse login(AdminDto.LoginRequest request);

    /**
     * 현재 관리자 정보 조회
     */
    AdminDto.MeResponse getMe(String adminId);

    /**
     * 대시보드
     */
    DashboardDto getDashboard();

    /**
     * 관리자 회원 목록 조회
     */
    List<AdminUserDto> getUsers();

    /**
     * 관리자 회원 상세 조회
     */
    AdminUserDetailDto getUserDetail(Long userId);
}
