/**Copyright (C) 2011 the University of Pittsburgh
 * Author: Thomas Maher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package edu.pitt.dbmi.facebase.hd.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.apache.log4j.Logger;

/**
* A POJO class to hold the data for a single key item.
* Annotated so its persistence can be managed by Hibernate.
*
* @author SHIREY
*/
@Entity(name="FbKey")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@Table(name="fb_keychain")
public class FbKey
{
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.domain.FbKey.class");
    private Long kid = null;
    private Integer uid = null;
    private String encryption_key = null;
    private Long created = null;
    
    public FbKey() {
        log.debug("FbKey constructed");
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kid", unique = true, nullable = false)
    public Long getKid()
    {
        return(kid);
    }
    public void setKid(Long kidval)
    {
        kid = kidval;
    }
    
    public Integer getUid()
    {
        return(uid);
    }
    public void setUid(Integer uidval)
    {
        uid = uidval;
    }
    public String getEncryption_key()
    {
        return(encryption_key);
    }
    public void setEncryption_key(String val)
    {
        encryption_key = val;
    }
    public Long getCreated()
    {
        return(created);
    }
    public void setCreated(Long val)
    {
        created = val;
    }
}
