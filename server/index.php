<?php

// Copyright 2011 Alex Dementsov

$key        = "d3rw56h8";
$actions    = array("get", "getall", "remove", "set", "size", "empty");

class Handler
{
    private $db;

    public function __construct(){
        $this->open_db();
    }

    function run()
    {
        global $key, $actions;

        if (!(isset($_GET["key"]) && $_GET["key"] == $key) || 
            !(isset($_GET["action"]) && in_array($_GET["action"], $actions)))
        {
            echo "Wrong key or action";
            return;
        }

        switch($_GET["action"])
        {
            case "get":
                return $this->get();
            case "getall":
                return $this->get_all();
            case "remove":
                return $this->remove();
            case "set":
                return $this->set();
            case "size":
                return $this->size();
            case "empty":
                return $this->isempty();
        }
    }
    function get()
    {
        if (!isset($_GET["cell"]))
            return $this->status_fail();

        $row    = $this->getValue();
        if (!$row)
            return $this->status_fail();

        return json_encode(array("cell" => $row["cid"], "value" => $row["cell"]));
    }


    function get_all()
    {
        $array  = array(-1, -1, -1, -1);
        $rows   = $this->getValues();
        if (!$rows)
            return $this->status_fail();

        foreach ($rows as $i => $value)
        {
            $idx = $value["cid"];
            if ($idx >= 0 && $idx < 4)
                $array[$idx] = $value["cell"];
        }
        return json_encode(array("cells" => $array));
    }

    function remove()
    {
        if (!isset($_GET["cell"]))
            return $this->status_fail();

        return $this->setValue(-1);
    }

    function set()
    {
        if (!isset($_GET["cell"]) or !isset($_GET["value"]))
            return $this->status_fail();

        return $this->setValue($_GET["value"]);
    }

    private function update($arr)
    {
        // Executes query and returns status
        $query  = "update cells set cell=? where cid=?";
        $q      = $this->db->prepare($query);
        $q->execute($arr) or die(print_r($this->db->errorInfo(), true));
        return $q;
    }

    private function getValue()
    {
        // Returns value of a cell
        $result = $this->db->query("select * from cells where cid=".$_GET["cell"]) or die($this->status_fail());
        return $result->fetch();
    }

    private function getValues()
    {
        // Returns value of a cell
        $result = $this->db->query("select * from cells limit 4") or die($this->status_fail());
        return $result->fetchAll();
    }    

    private function setValue($value)
    {
        $arr    = array($value, $_GET["cell"]);
        $q      = $this->update($arr);
        if (!$q)
            return $this->status_fail();

        return $this->status_ok();
    }

    function size()
    {
        return json_encode(array("size" => 4));
    }

    function isempty()
    {
        $row    = $this->getValue();
        if (!$row)
            return $this->status_fail();
        
        if ($row["cell"] == -1)
            return json_encode(array("empty" => True));

        return json_encode(array("empty" => False));
    }

    private function status_fail()
    {
        return $this->status("fail");
    }

    private function status_ok()
    {
        return $this->status("ok");
    }

    private function status($status)
    {
        return json_encode(array("status" => $status));
    }

    private function open_db()
    {
        try {
            $this->db = new PDO('sqlite:db/cells.db');
        }
        catch(Exception $e) {
            die($this->status_fail());
        }
    }
}

    
$handler = new Handler();
echo $handler->run();
?>
