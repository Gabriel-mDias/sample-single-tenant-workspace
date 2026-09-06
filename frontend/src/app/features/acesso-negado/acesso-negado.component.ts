import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { GemsButtonComponent } from '@gabriel-mdias/angular-gems-sdk';

@Component({
  selector: 'app-acesso-negado',
  imports: [GemsButtonComponent],
  templateUrl: './acesso-negado.component.html',
  styleUrl: './acesso-negado.component.css',
})
export class AcessoNegadoComponent {
  protected readonly router = inject(Router);
}
