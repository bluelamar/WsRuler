package org.bluelamar.wsruler.svr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bluelamar.wsruler.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.inject.Inject;

@Path("/v1")
public class WsSvrResources {
	
	// entity fields
	//
	static final String ENT_FIELD_ID = "id";
	static final String ENT_FIELD_NAME = "name";
	static final String ENT_FIELD_PAR = "parent";
	static final String ENT_FIELD_DLINK = "data_link";
	static final String ENT_FIELD_TYPE = "type";
	static final String ENT_FIELD_GRPS = "groups";
	static final String ENT_FIELD_OWNERS = "owners";
	static final String ENT_FIELD_EMAIL = "email_address";

	@GET
	@Path("/db/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public WsEntity getDbEntity(@PathParam("id") String id) {
		return getEntity("db", id);
	}
	
	@GET
	@Path("/env/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public WsEntity getEnvEntity(@PathParam("id") String id) {
		return getEntity("env", id);
	}
	
	@GET
	@Path("/repo/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public WsEntity getRepoEntity(@PathParam("id") String id) {
		return getEntity("repo", id);
	}
	
	@PUT
	@Path("/db/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void putDbEntity(@PathParam("id") String id, WsEntity entity) {
		putEntity("db", id, entity);
	}
	
	@PUT
	@Path("/env/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void putEnvEntity(@PathParam("id") String id, WsEntity entity) {
		putEntity("env", id, entity);
	}
	
	@PUT
	@Path("/repo/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void putRepoEntity(@PathParam("id") String id, WsEntity entity) {
		putEntity("repo", id, entity);
	}
	
    @DELETE
    @Path("/db/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteDbEntity(@PathParam("id") String id) {
		try {
			this.delegate.deleteEntity("db", id);
			// find db links that have data_link == id and delete them
			List<WsLink> links = getLinksForEntity("db", id);
			if (links != null) {
				for (WsLink link: links) {
					String lid = link.getId();
					deleteDbLink(lid);
				}
			}
		} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, 500);
		}
    }
    
    @DELETE
    @Path("/env/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteEnvEntity(@PathParam("id") String id) {
		try {
			this.delegate.deleteEntity("env", id);
			// find env links that have data_link == id and delete them
			// find db links that have parent == id and delete those links
			deleteEnvLink(id);
			
			List<Object> res = this.delegate.getChildren("env", id);
			List<WsLink> links = processLinkList(res);
			if (res != null) {
				for (WsLink link: links) {
					String lid = link.getId();
					deleteDbLink(lid);
				}
			}
		} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    
    @DELETE
    @Path("/repo/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteRepoEntity(@PathParam("id") String id) {
		try {
			this.delegate.deleteEntity("repo", id);
			// find repo links that have data_link == id and delete them
			List<WsLink> links = getLinksForEntity("repo", id);
			if (links != null) {
				for (WsLink link: links) {
					String lid = link.getId();
					deleteDbLink(lid);
				}
			}
		} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
	
	@GET
	@Path("/link/db/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public WsLink getDbLink(@PathParam("id") String id) {
		return getLink("db", id);
	}
	
	@GET
	@Path("/link/env/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public WsLink getEnvLink(@PathParam("id") String id) {
		return getLink("env", id);
	}
	
	@GET
	@Path("/link/repo/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public WsLink getRepoLink(@PathParam("id") String id) {
		return getLink("repo", id);
	}
	
	@GET
	@Path("/env/children/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<WsEntity> getEnvChildren(@PathParam("id") String id) {
		
		return getChildren("env", "db", id);
	}
	
	@GET
	@Path("/ws/children/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<WsEntity> getWsChildren(@PathParam("id") String id) {
		// FIX @todo need to get the db children for the env children
		return getChildren("ws", "env", id);
	}

	@POST
	@Path("/link/db")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public WsLink postDbLink(WsLink link) {
		validateLinks("db", "env", link);
		return postLink("db", link);
    }
	
	@POST
	@Path("/link/env")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public WsLink postEnvLink(WsLink link) {
		validateLinks("env", "ws", link);
		return postLink("env", link);
    }
	
	@POST
	@Path("/link/repo")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public WsLink postRepoLink(WsLink link) {
		validateLinks("repo", "ws", link);
		return postLink("repo", link);
    }
	
	@PUT
	@Path("/link/db/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void putDbLink(@PathParam("id") String id, WsLink link) {
		validateLinks("db", "env", link);
		putLink("db", id, link);
	}
	
	@PUT
	@Path("/link/env/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void putEnvLink(@PathParam("id") String id, WsLink link) {
		validateLinks("env", "ws", link);
		putLink("env", id, link);
	}
	
	@PUT
	@Path("/link/repo/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public void putRepoLink(@PathParam("id") String id, WsLink link) {
		validateLinks("repo", "ws", link);
		putLink("repo", id, link);
	}

    @DELETE
    @Path("/link/db/{id}")
    public void deleteDbLink(@PathParam("id") String id) {
		try {
			this.delegate.deleteEntity("db", id);
		} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    
    @DELETE
    @Path("/link/repo/{id}")
    public void deleteRepoLink(@PathParam("id") String id) {
		try {
			this.delegate.deleteEntity("repo", id);
		} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    
    @DELETE
    @Path("/link/env/{id}")
    public void deleteEnvLink(@PathParam("id") String id) {
		try {
			this.delegate.deleteEntity("env", id);
			List<Object> res = this.delegate.getChildren("env", id);
			if (res == null || res.isEmpty()) {
				return;
			}
			List<WsLink> links = processLinkList(res);
			if (links != null) {
				for (WsLink link: links) {
					String lid = link.getId();
					deleteDbLink(lid);
				}
			}
		} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    
    WsEntity getEntity(String comp, String id) {
		try {
			Map<String,Object> entity = new HashMap<>();
			entity = this.delegate.getEntity(comp, id);
			WsEntity wsentity = new WsEntity();
			wsentity.setId(entity.get(ENT_FIELD_ID).toString());
			Object entFld = entity.get(ENT_FIELD_NAME);
			if (entFld != null) {
				wsentity.setName(entFld.toString());
			}
			return wsentity;
		} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    
    WsEntity postEntity(String comp, WsEntity wsentity) {
 		try {
 			Map<String,Object> entity = new HashMap<>();
 			entity.put(ENT_FIELD_NAME, wsentity.getName());
 			entity = this.delegate.postEntity(comp, entity);
 			wsentity.setId(entity.get(ENT_FIELD_ID).toString());
 			wsentity.setName(entity.get(ENT_FIELD_NAME).toString());
 			return wsentity;
 		} catch (ConnException ex) {
 			ex.printStackTrace();
 			throw new WebApplicationException(ex, ex.getErrorCode());
 		}
    }
    
	void putEntity(String comp, String id, WsEntity wsentity) {
		try {
			Map<String,Object> entity = new HashMap<>();
			entity.put(ENT_FIELD_NAME, wsentity.getName());
			this.delegate.putEntity(comp, id, entity);
		} catch (ConnException ex) {
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
	}
    
    WsLink postLink(String comp, WsLink link) {
		try {
			Map<String,Object> entity = new HashMap<>();
			entity.put(ENT_FIELD_DLINK, link.getData_link());
			entity.put(ENT_FIELD_PAR, link.getParent());
			entity = this.delegate.postEntity(comp, entity);
			link.setId(entity.get(ENT_FIELD_ID).toString());
			link.setData_link(entity.get(ENT_FIELD_DLINK).toString());
			link.setParent(entity.get(ENT_FIELD_PAR).toString());
			return link;
		} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    
	void putLink(String comp, String id, WsLink link) {
		try {
			Map<String,Object> entity = new HashMap<>();
			entity.put(ENT_FIELD_DLINK, link.getData_link());
			entity.put(ENT_FIELD_PAR, link.getParent());
			this.delegate.putEntity(comp, id, entity);
		} catch (ConnException ex) {
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
	}
    
    WsLink getLink(String comp, String id) {
		try {
			Map<String,Object> entity = new HashMap<>();
			entity = this.delegate.getEntity(comp, id);
			WsLink link = new WsLink();
			link.setId(entity.get(ENT_FIELD_ID).toString());
			Object entFld = entity.get(ENT_FIELD_DLINK);
			if (entFld != null) {
				link.setData_link(entFld.toString());
			}
			entFld = entity.get(ENT_FIELD_PAR);
			if (entFld != null) {
				link.setParent(entFld.toString());
			}
			return link;
		} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    
    void validateLinks(String dlComp, String plComp, WsLink link) {
    	try {
	    	Map<String,Object> entity = this.delegate.getEntity(dlComp, link.getData_link());
	    	// find the parent for this entity
	    	entity = this.delegate.getEntity(plComp, link.getParent());
    	} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    
    List<WsLink> getLinksForEntity(String comp, String id) {
    	
    	try {
    		List<Object> res = this.delegate.getEntities(comp, ENT_FIELD_DLINK, id);
    		List<WsLink> links = processLinkList(res);
    		return links;
    	} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    List<WsEntity> getChildren(String pComp, String dComp, String id) {
    	
    	try {
	    	List<Object> res = this.delegate.getChildren(pComp, id);
	    	List<WsLink> links = processLinkList(res);
	    	// use the data_link for each returned link to get the entity
	    	List<WsEntity> entities = new ArrayList<>();
	    	if (links != null) {
	    		for (WsLink link: links) {
	    			String entityId = link.getData_link();
	    			System.out.println("FIX rsrc: datalink=" + entityId + " par="+ link.getParent());
	    			WsEntity entity = getEntity(dComp, entityId);
	    			entities.add(entity);
	    		}
	    	}
    		return entities;
    	} catch (ConnException ex) {
			ex.printStackTrace();
			throw new WebApplicationException(ex, ex.getErrorCode());
		}
    }
    
    List<WsLink> processLinkList(List<Object> res) {
    	
		List<WsLink> linkList = new ArrayList<>();
		if (res != null) {
			for (Object obj: res) {
 				System.out.println("FIX rsrc: proclinklist: " + obj);
				if (obj instanceof Map) {
					// create a WsLink from the map
					Map<String,Object> mapObj = (Map<String,Object>)obj;
					WsLink link = new WsLink();
					link.setId(mapObj.get("_id").toString());
					Object resObj = mapObj.get(ENT_FIELD_DLINK);
					if (resObj != null) {
						link.setData_link(resObj.toString());
					}
					resObj = mapObj.get(ENT_FIELD_PAR);
					if (resObj != null) {
						link.setParent(resObj.toString());
					}
					linkList.add(link);
				}
			}
		}
		return linkList;
	}

	@Inject private WsSvrHandler delegate;
	@Context private HttpServletRequest request;
	@Context private HttpServletResponse response;
}