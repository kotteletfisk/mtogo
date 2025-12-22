/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mtogo.sql.ports.out;

import java.util.List;

import mtogo.sql.DTO.AuthDTO;

/**
 *
 * @author kotteletfisk
 */
public interface IAuthRepository {

    public AuthDTO fetchAuthPerEmail(String email);

    public List<String> fetchRolesForAuth(long id);

    public String fetchActorIdForAuth(String cred, String service);
}
