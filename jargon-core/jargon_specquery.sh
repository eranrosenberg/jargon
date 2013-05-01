#!/bin/sh

iadmin asq "select alias,sqlStr from R_SPECIFIC_QUERY where alias like ?" listQueryByAliasLike

iadmin asq "select alias,sqlStr from R_SPECIFIC_QUERY where alias = ?" findQueryByAlias

iadmin asq "SELECT c.parent_coll_name, c.coll_name, c.create_ts, c.modify_ts, c.coll_id, c.coll_owner_name, c.coll_owner_zone, c.coll_type, u.user_name, u.zone_name, a.access_type_id, u.user_id FROM r_coll_main c JOIN r_objt_access a ON c.coll_id = a.object_id JOIN r_user_main u ON a.user_id = u.user_id WHERE c.parent_coll_name = ? LIMIT ? OFFSET ?" ilsLACollections


