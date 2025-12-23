/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package mtogo.sql.ports.out;

import java.io.IOException;

/**
 *
 * @author kotteletfisk
 */
public interface IRpcResponder {
    void reply(Object response) throws IOException;
    void replyError() throws IOException;
}
