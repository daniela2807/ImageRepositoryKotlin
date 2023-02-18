<?php
    $conexion = mysqli_connect("localhost","root","root","tabla") ;

    if($conexion){
        echo "Conexion exitosa";
    }else{
        echo "Fallo la conexion";
    }
?>