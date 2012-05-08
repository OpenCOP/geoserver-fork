
CREATE TABLE "Shelters_List"
(
  dataid integer,
  incidentid integer,
  userid integer,
  positionid integer,
  prevdataid integer,
  entrydate timestamp without time zone,
  globalid integer,
  name text,
  address text,
  country text,
  capacity integer,
  occupancy text,
  status text,
  arc text,
  specialneeds text,
  petfriendly boolean,
  title text,
  contactname text,
  contactnumber text,
  remarks text,
  _sys_latitude double precision,
  _sys_longitude double precision,
  label text,
  latitude double precision,
  longitude double precision,
  subscribername text
)
WITH (
  OIDS=FALSE
);
ALTER TABLE "Shelters_List"
  OWNER TO postgres;