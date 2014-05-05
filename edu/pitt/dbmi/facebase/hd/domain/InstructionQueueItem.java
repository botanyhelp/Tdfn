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
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.apache.log4j.Logger;

/**
* A POJO class to hold the data for a single queue item.
* Annotated so its persistence can be managed by Hibernate.
*
* @author SHIREY
*/
@Entity(name="QueueItem")
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@Table(name="fb_queue")
public class InstructionQueueItem
{
    private static final Logger log = Logger.getLogger("edu.pitt.dbmi.facebase.hd.domain.InstructionQueueItem.class");
    private Long qid = null;
    private Integer uid = null;
    private Integer eid = null;
    private String hash = null;
    private String status = null;
    private String name = null;
    private String description = null;
    private String instructions = null;
    private String results = null;
    private Integer hits = null;
    private Long created = null;
    private Long received = null;
    private Long completed = null;
    private Long accessed = null;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qid", unique = true, nullable = false)
    public Long getQid()
    {
        return(qid);
    }

    public void setQid(Long qidval)
    {
        qid = qidval;
    }
    
    public Integer getHits()
    {
        return(hits);
    }

    public void setHits(Integer hitsval)
    {
        hits = hitsval;
    }
    
    public Integer getEid()
    {
        return(eid);
    }

    public void setEid(Integer eidval)
    {
        eid = eidval;
    }
    
    public Integer getUid()
    {
        return(uid);
    }

    public void setUid(Integer uidval)
    {
        uid = uidval;
    }

    public String getHash()
    {
        return(hash);
    }

    public void setHash(String val)
    {
        hash = val;
    }

    public String getInstructions()
    {
        return(instructions);
    }

    public void setInstructions(String val)
    {
        instructions = val;
    }
    
    public Long getCreated()
    {
        return(created);
    }

    public void setCreated(Long val)
    {
        created = val;
    }
    
    public Long getReceived()
    {
        return(received);
    }

    public void setReceived(Long val)
    {
        received = val;
    }
    
    
    public Long getCompleted()
    {
        return(completed);
    }

    public void setCompleted(Long val)
    {
        completed = val;
    }
    
    public Long getAccessed()
    {
        return(accessed);
    }

    public void setAccessed(Long val)
    {
        accessed = val;
    }

    /*    @Column(name="btest")
    public String getBlob()
    {
        return(blob);
    }

    public void setBlob(String val)
    {
        blob = val;
    } */

    public String getStatus()
    {
        return(status);
    }

    public void setStatus(String val)
    {
        status = val;
    }
    
    public String getName()
    {
        return(name);
    }

    public void setName(String val)
    {
        name = val;
    }
    
    public String getDescription()
    {
        return(description);
    }

    public void setDescription(String val)
    {
        description = val;
    }
    
    public String getResults()
    {
        return(results);
    }

    public void setResults(String val)
    {
        results = val;
    }
}
