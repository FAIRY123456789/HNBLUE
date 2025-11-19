package com.example.jpaspringboot.service;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public interface AdminService {

   void generateAndSaveTestAdmins();


    @ResponseBody int addAdmin(@RequestParam String name, @RequestParam String password);
}
