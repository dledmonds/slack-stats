package com.dledmonds.slack;

import com.github.seratch.jslack.api.model.User;

/**
 * @author dledmonds
 */
public interface UserProcessor {

    void seenUser(User user);

}
