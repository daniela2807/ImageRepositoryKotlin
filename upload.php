<?php

include('cn.php');

if($_SERVER['REQUEST_METHOD']=='POST'){
 
    $imagen= $_POST['foto'];
    $nombre = $_POST['nombre'];

    // RUTA DONDE SE GUARDARAN LAS IMAGENES
    $path = "uploads/$nombre.png";

    $actualpath = "http://localhost/prueba/$path";

    $sql = "INSERT INTO imagenes (id,foto,nombre) values ('NULL','$actualpath','$nombre')";

    if(mysqli_query($conexion,$sql)){
        file_put_contents($path, base64_decode($imagen));
        echo "Se subio exitosamente";
        mysqli_close($conexion);
    }

}else{
    echo "Error";
}

?>