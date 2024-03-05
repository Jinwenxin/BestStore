package com.cafebabe.beststore.controllers;

import com.cafebabe.beststore.models.Product;
import com.cafebabe.beststore.models.ProductDto;
import com.cafebabe.beststore.services.ProductsRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductsController {
    @Autowired
    private ProductsRepository repo;

    @GetMapping({"","/"})
    public String showProductList(Model model){
        List<Product> products = repo.findAll(Sort.by(Sort.Direction.DESC,"id"));
        model.addAttribute("products",products);
        return "products/index";

    }

    @GetMapping({"/create"})
    public String showCreatePage(Model model){
        ProductDto dto=new ProductDto();
        model.addAttribute("productDto",dto);
        return "products/CreateProduct";

    }

    @GetMapping("/edit")
    public String showEditPage(Model model, @RequestParam int id){
        try{
            Product product = repo.findById(id).get();
            ProductDto productDto = new ProductDto();
            productDto.setBrand(product.getBrand());
            productDto.setName(product.getName());
            productDto.setCategory(product.getCategory());
            productDto.setDescription(product.getDescription());
            productDto.setPrice(product.getPrice());
            model.addAttribute("product", product);
            model.addAttribute("productDto",productDto);
        } catch (Exception ex){
            System.out.println("Exception:"+ex.getMessage());
            return "redirect:/products";
        }
        return "products/EditProduct";
    }

    @PostMapping("/edit")
    public String editProduct(Model model, @RequestParam int id,@Valid @ModelAttribute ProductDto productDto,BindingResult result){
//        if(productDto.getImageFile().isEmpty()){
//            result.addError(new FieldError("productDto","imageFile","the image file is empty"));
//        }
        if(result.hasErrors()){
            return "products/EditProduct";
        }
        Product product_old = repo.findById(id).get();
        //model.addAttribute("product",product_old);
        if(!productDto.getImageFile().isEmpty()){
            // delete old image
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir + product_old.getImageFileName());
            if(Files.exists(uploadPath)){
                try {
                    Files.delete(uploadPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
            // save new image
            MultipartFile image = productDto.getImageFile();
            Date createdAt = new Date();
            String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();
            try(InputStream inputStream = image.getInputStream()){
                Files.copy(inputStream,Paths.get(uploadDir+storageFileName), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            product_old.setImageFileName(storageFileName);
        }
            // save product in db
        product_old.setId(id);
        product_old.setName(productDto.getName());
        product_old.setBrand(productDto.getBrand());

        product_old.setDescription(productDto.getDescription());
        product_old.setPrice(productDto.getPrice());
        product_old.setCategory(productDto.getCategory());
            repo.save(product_old);

        return "redirect:/products";
    }

    @PostMapping({"/create"})
    public String createProduct(@Valid @ModelAttribute ProductDto productDto, BindingResult result){
        if(productDto.getImageFile().isEmpty()){
            result.addError(new FieldError("productDto","imageFile","the image file is empty"));
        }
        if(result.hasErrors()){
            return "products/CreateProduct";
        }
        // save image
        MultipartFile image = productDto.getImageFile();
        Date createdAt = new Date();
        String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

        try{
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);
            if(!Files.exists(uploadPath)){
                Files.createDirectories(uploadPath);
            }
            try(InputStream inputStream = image.getInputStream()){
                Files.copy(inputStream,Paths.get(uploadDir+storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex){
            System.out.println("Exception:" + ex.getMessage());

        }
        // save product in db
        Product product = new Product();
        product.setName(productDto.getName());
        product.setBrand(productDto.getBrand());
        product.setCreatedAt(createdAt);
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setCategory(productDto.getCategory());
        product.setImageFileName(storageFileName);

        repo.save(product);
        return "redirect:/products";

    }
    @GetMapping("/delete")
    public String deleteProduct(Model model,@RequestParam int id){
        repo.deleteById(id);
        return "redirect:/products";
    }




}
